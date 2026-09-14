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

**Prerequisites:** Java 21, Maven (or use the bundled `./mvnw`), AWS credentials resolvable via the SDK's default credential provider chain (e.g. `~/.aws/credentials`) if you want to exercise the AWS-backed endpoints locally.

1. Edit `src/main/resources/application-local.yml` (gitignored — put real secrets directly in it, Spring Boot does not read `.env` files) with your real API keys:

   ```yaml
   pubg:
     api:
       key: <your PUBG API key>
   gemini:
     api:
       key: <your Gemini API key>
   ```

   `spring.profiles.active: local` is already the default in `application.yml`, so this file is picked up automatically — no extra flag needed.

2. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

3. The backend runs on `http://localhost:8080`.

---

## Deployment

`deploy.sh` builds and deploys the backend to the existing Elastic Beanstalk environment:

```bash
./deploy.sh
```

It runs `mvn clean package` (requires real Maven Central network access), uploads the resulting jar to S3, creates a new Elastic Beanstalk application version, and updates the `Pubg-insight-backend-env` environment to that version.

The Lambda (`lambda/share-analysis/`) and the ECS Fargate job (`season-stats-cache-warmer/`) are deployed and scheduled separately — see `docs/deliverables/ARCHITECTURE.md` for details.

---

## Further Reading

For full architecture — system context, component diagrams, per-feature sequence diagrams, data mapping, error-handling flow, and design-decision rationale — see **[`docs/deliverables/ARCHITECTURE.md`](docs/deliverables/ARCHITECTURE.md)**.
