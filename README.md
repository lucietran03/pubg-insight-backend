# PUBG Insight Backend

Backend service for **PUBG Insight — AI-powered Performance Analytics Platform**.

A Spring Boot REST API that turns raw PUBG match/season data into readable performance insights: it fetches player and match data from the PUBG Developer API, generates AI summaries via Google Gemini, caches and persists results across a set of AWS services, and serves everything to the [React frontend](../pubg-insight-frontend).

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (Spring MVC / `RestClient`, synchronous) |
| Build Tool | Maven |
| Validation | Jakarta Validation |
| AWS SDK | AWS SDK v2 |
| AI | Google Gemini API |
| External API | PUBG Developer API |
| Deployment | AWS Elastic Beanstalk (behind CloudFront) |

---

## Architecture

The backend is organized **by feature, not by technical layer**, under `src/main/java/com/pubginsight/`:

```
com.pubginsight
├── player/              # player search, season stats
├── match/               # match analytics, weapon breakdown, Athena population comparison
├── insight/             # AI-generated insights (composes player + match, calls Gemini)
├── history/             # saved analysis history (composes match + insight, backed by DynamoDB)
├── health/              # health check endpoint
├── client/
│   ├── pubg/            # PUBG Developer API integration
│   ├── gemini/          # Google Gemini API integration
│   ├── s3/              # S3 match-cache client
│   ├── dynamodb/        # DynamoDB analysis-history / season-stats-cache client
│   ├── athena/          # Athena analytics-query client
│   └── telemetry/       # PUBG match telemetry (weapon/kill/hit events)
└── common/
    ├── config/          # cross-cutting config (CORS, etc.)
    └── exception/       # global exception handling
```

Feature packages own their own controller, service, mapper, DTOs, and exceptions; `client/*` packages are pure external-integration wrappers with no feature-specific logic. `insight` and `history` are composition features that call other features' services directly rather than duplicating logic.

Each feature package's controller is a good jumping-off point for the API surface: `PlayerController`, `MatchController`, `WeaponBreakdownController`, `InsightController`, `HistoryController`, `HealthController`.

For the full system diagrams (sequence flows, data mapping, design-decision rationale) see **[`docs/deliverables/ARCHITECTURE.md`](docs/deliverables/ARCHITECTURE.md)**.

### AWS services

This backend integrates with AWS services across six categories, each invoked by real application code (not console-only setup):

| Service | Why |
|---|---|
| **Elastic Beanstalk** | Hosts the backend itself (`Pubg-insight-backend-env`). |
| **Lambda + API Gateway** | Standalone Node.js Lambda (`lambda/share-analysis/`) behind an HTTP API serves a public, shareable "report card" for a saved analysis — `GET /share/{playerId}/{matchId}` — reading DynamoDB directly, independent of the main app. |
| **ECS Fargate + EventBridge Scheduler** | A standalone containerized job (`season-stats-cache-warmer/`) runs on a schedule to proactively refresh cached season stats for recently-searched players, reducing live PUBG API calls. |
| **S3** | `pubg-insight-frontend` bucket hosts the static frontend; `pubg-insight-match-cache` bucket holds the raw match cache, a flat analytics feed, and Athena query results. |
| **CloudFront** | Two distributions — one fronting the S3 frontend, one fronting the Elastic Beanstalk backend — for HTTPS and edge caching. |
| **DynamoDB** | `pubg-insight-analysis-history` (saved analyses) and `pubg-insight-season-stats-cache` (1h TTL cache for PUBG season stats). |
| **Athena + Glue** | Queries an external table over the S3 analytics feed to compute cross-player damage percentiles, exposed via a backend endpoint (`/population-comparison`). |
| **PUBG Developer API / Google Gemini API** | Third-party APIs: raw player/match data, and AI-generated performance insights. |

---

## API Overview

All endpoints are under `/api`. See the controllers listed above for full request/response shapes, or the sequence diagrams in `ARCHITECTURE.md` for end-to-end flow.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/health` | Health check |
| `GET` | `/api/players/{name}` | Search a player by in-game name |
| `GET` | `/api/players/by-id/{accountId}` | Look up a player by PUBG account id |
| `GET` | `/api/players/{playerId}/season-stats` | Current-season win rate / aggregate stats |
| `GET` | `/api/players/{playerId}/matches/{matchId}` | Match stats for a specific player |
| `GET` | `/api/players/{playerId}/matches/{matchId}/population-comparison` | Athena-backed damage percentile vs. all cached matches |
| `GET` | `/api/players/{playerId}/matches/{matchId}/weapons` | Weapon/kill breakdown from match telemetry |
| `GET` | `/api/players/{playerId}/matches/{matchId}/insights` | AI-generated insight summary (Gemini) |
| `POST` | `/api/players/{playerId}/matches/{matchId}/history` | Save a match + insight to analysis history (DynamoDB) |
| `GET` | `/api/players/{playerId}/history` | List saved analysis history for a player |

A separate, Spring-independent endpoint is served by the Lambda: `GET /share/{playerId}/{matchId}` (public, read-only, backed directly by DynamoDB).

---

## Getting Started / Local Setup

**Prerequisites:** Java 21, Maven (or use the bundled `./mvnw`), an AWS account with credentials resolvable via the SDK's default credential provider chain (e.g. `~/.aws/credentials`) — required for every endpoint except plain player/match/season lookups, since those already touch S3/DynamoDB/Athena.

1. **Create** `src/main/resources/application-local.yml` (this file does not exist in a fresh clone — it's gitignored on purpose, since it holds real secrets) with your own API keys:

   ```yaml
   pubg:
     api:
       key: <your PUBG API key>
   gemini:
     api:
       key: <your Gemini API key>
   ```

   Get a PUBG API key from https://developer.pubg.com (free, requires a PUBG account). Get a Gemini API key from https://aistudio.google.com/apikey (free tier available). `spring.profiles.active: local` is already the default in `application.yml`, so this file is picked up automatically — no extra flag needed. Spring Boot does **not** read `.env` files (that's a Node/Vite convention), so put the real values directly in this YAML file, not a `.env`.

2. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

3. The backend runs on `http://localhost:8080`.

Running locally with no AWS credentials configured still works for basic player/match/season lookups — S3/DynamoDB/Athena calls soft-fail (logged, not thrown) wherever the code treats them as a cache rather than a source of truth (see `docs/deliverables/ARCHITECTURE.md` Design Decision D12). The Athena population-comparison and history endpoints do require real AWS credentials and the resources below to exist.

---

## AWS Resources Required (one-time setup)

Everything below is a **one-time provisioning step** — allowed under the assignment's automation rule, since none of it happens in the live request path (see `docs/deliverables/ARCHITECTURE.md` §7). Once created, every read/write against these resources happens through application code. Region used throughout: `us-east-1`.

| # | Service | Resource | Purpose |
|---|---|---|---|
| 1 | S3 | `pubg-insight-match-cache` bucket | raw match cache (`matches/`), Athena feed (`analytics/`), Athena results (`athena-results/`) |
| 2 | S3 | `pubg-insight-frontend` bucket, static website hosting enabled | serves the built frontend |
| 3 | DynamoDB | `pubg-insight-analysis-history` table (PK `playerId`, SK `matchId`) | saved analyses |
| 4 | DynamoDB | `pubg-insight-season-stats-cache` table (PK `playerId`, TTL `expiresAt`) | 1h season-stats cache |
| 5 | Elastic Beanstalk | application `pubg-insight-backend` + environment (Java/Corretto platform) | hosts this backend |
| 6 | IAM | `aws-elasticbeanstalk-ec2-role` — `AmazonS3FullAccess`, `AmazonDynamoDBFullAccess`, inline policy for `athena:StartQueryExecution/GetQueryExecution/GetQueryResults` + `glue:GetTable/GetDatabase` scoped to `pubg_insight` | lets the running backend call S3/DynamoDB/Athena |
| 7 | CloudFront | distribution fronting the S3 frontend bucket (custom HTTP origin, redirect-to-https) | HTTPS + edge caching for the SPA |
| 8 | CloudFront | distribution fronting the Elastic Beanstalk backend (custom HTTP origin, CachingDisabled + AllViewer policy) | fixes browser mixed-content blocking |
| 9 | Athena | database `pubg_insight`, external table `pubg_insight_analytics` (manual DDL, `org.openx.data.jsonserde.JsonSerDe`, location `s3://pubg-insight-match-cache/analytics/`) | population-comparison queries |
| 10 | Lambda | function `pubg-insight-share-analysis` (Node.js 20.x, source `lambda/share-analysis/index.mjs`) | serves the public share page |
| 11 | IAM | `pubg-insight-share-lambda-role` — `dynamodb:GetItem` on `pubg-insight-analysis-history` only | least-privilege role for the Lambda above |
| 12 | API Gateway | HTTP API with route `GET /share/{playerId}/{matchId}` → the Lambda above, plus a resource-based Lambda permission for `apigateway.amazonaws.com` | public share URL |
| 13 | ECR | repository `pubg-insight-season-stats-cache-warmer` | holds the cache-warmer container image |
| 14 | ECS | cluster `pubg-insight`, Fargate task definition `pubg-insight-season-stats-cache-warmer` (source `season-stats-cache-warmer/`) | scheduled cache-warming job |
| 15 | IAM | `ecsTaskExecutionRole` (standard `AmazonECSTaskExecutionRolePolicy`), `pubg-insight-season-stats-warmer-task-role` (`dynamodb:Scan` on analysis-history only) | ECS execution + task roles |
| 16 | EventBridge Scheduler | schedule `pubg-insight-season-stats-cache-warmer`, `rate(6 hours)`, target = the ECS task above | triggers the cache warmer automatically |
| 17 | IAM | `pubg-insight-scheduler-ecs-role` — `ecs:RunTask` + `iam:PassRole`, scoped to the cluster/task-definition/roles above | lets EventBridge Scheduler start the ECS task |

Full AWS CLI commands used to provision every resource above are preserved in this repository's commit history (each addition landed as its own commit around the corresponding feature — e.g. search commit messages for "Athena", "CloudFront", "ECS Fargate"). There is no single setup script that runs all of them end-to-end; they were created incrementally as each AWS category was added.

---

## Deployment

Once the resources above exist, `deploy.sh` (repo root) builds and ships the **backend** to the existing Elastic Beanstalk environment:

```bash
./deploy.sh
```

It runs `mvn clean package` (requires real Maven Central network access) → uploads the resulting jar to the EB-managed S3 bucket → `aws elasticbeanstalk create-application-version` → `aws elasticbeanstalk update-environment` → waits for the environment to report `Ready`/`Green`.

The **frontend** deploys separately (from the `pubg-insight-frontend` repo):

```bash
npm run build
aws s3 sync dist/ s3://pubg-insight-frontend/ --delete
aws cloudfront create-invalidation --distribution-id <frontend-distribution-id> --paths "/*"
```

The **Lambda** (`lambda/share-analysis/`) redeploys with a plain zip upload — no build step, since it has no npm dependencies (Lambda's Node.js 20.x runtime already bundles the AWS SDK v3 packages it imports):

```bash
cd lambda/share-analysis
zip -q function.zip index.mjs
aws lambda update-function-code --function-name pubg-insight-share-analysis --zip-file fileb://function.zip
```

The **ECS cache-warmer container** (`season-stats-cache-warmer/`) redeploys by rebuilding and re-pushing its image, then letting the next scheduled run (or a manual `aws ecs run-task`) pick up `:latest`:

```bash
cd season-stats-cache-warmer
docker build --platform linux/amd64 -t pubg-insight-season-stats-cache-warmer:latest .
docker tag pubg-insight-season-stats-cache-warmer:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/pubg-insight-season-stats-cache-warmer:latest
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/pubg-insight-season-stats-cache-warmer:latest
```

None of these four deploy paths depend on each other — redeploying the backend does not require redeploying the Lambda or the ECS image, and vice versa.

---

## Further Reading

For full architecture — system context, component diagrams, per-feature sequence diagrams, data mapping, error-handling flow, and design-decision rationale — see **[`docs/deliverables/ARCHITECTURE.md`](docs/deliverables/ARCHITECTURE.md)**.
