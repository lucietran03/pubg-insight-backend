# Solution Architecture Document — PUBG Insight

> Submission-ready document for the assignment's Solution Architecture Document and Project Report deliverables (COSC2980 Assignment 3 — AWS Cloud System Development). Structured per the grading rubric: Summary, Introduction, Related Work, System Architecture, System Descriptions, Dataset/API Description, References. The technical companion document `docs/deliverables/ARCHITECTURE.md` contains additional per-feature sequence diagrams, data-mapping diagrams, error-handling flow, and the full design-decision log this document draws on.

---

## Summary

PUBG Insight is a cloud-native analytics platform that turns raw PUBG (PlayerUnknown's Battlegrounds) gameplay data into interpreted, actionable performance feedback. A React frontend lets a player search any username; a Spring Boot backend retrieves that player's profile, season statistics, and match history from the official PUBG Developer API, computes derived metrics (headshot rate, win rate, per-match placement and damage), and — on request — sends those aggregated metrics (never raw telemetry) to Google's Gemini API to generate a natural-language performance summary, strengths, weaknesses, and recommendations. The system is deployed entirely on AWS, spanning all six cloud-service categories required by the assignment: **Compute** (Elastic Beanstalk, and a second Compute integration via Lambda), **Containers** (ECS Fargate), **Storage** (S3), **Networking & Content Delivery** (CloudFront, API Gateway), **Database** (DynamoDB), and **Analytics** (Athena). Every one of these services is invoked automatically by the application's own code or a scheduled trigger — never a manual Console step in the live request path — and each closes a real gap identified earlier in the project's own design work, rather than being added purely to satisfy a rubric line item. The result is a single, automated pipeline — from a player typing a name to receiving hard statistics, AI-interpreted feedback, a cross-player population comparison, and a shareable public report — that replaces the manual, spreadsheet-style comparison players currently have to do themselves across existing stat-tracking sites.

---

## Introduction

### Motivations

Several third-party websites already expose PUBG statistics — kill counts, damage totals, survival time, win rate — pulled from the same official PUBG Developer API this project uses. What none of them do is *interpret* those numbers. A player is left to manually compare a handful of figures across matches and guess at what they mean: is a 21% headshot rate good for their rank? Is their damage output the reason they're not converting kills into wins, or is it their survival time? Against whom should any of these numbers even be judged, given that a single player's own history has no other players to compare against? This project's motivation is to remove that manual interpretation step entirely, and to close that comparison gap, by combining four things that are individually common but rarely combined for this purpose: (1) official, authoritative gameplay data from PUBG's own API, (2) cloud-based automated data processing and persistence so a player's history accumulates and can be trended and compared over time, (3) an AI layer that converts the resulting aggregated numbers into the kind of plain-language "here's what's going well, here's what isn't, here's what to work on" feedback a human coach would give, and (4) a genuine population of previously analyzed matches — accumulated automatically as a byproduct of ordinary use — that a new result can be measured against, instead of an arbitrary fixed benchmark.

### What it does

At a high level, PUBG Insight is a two-repository web application (a React frontend, a Spring Boot backend) backed entirely by AWS cloud infrastructure. A user searches for a PUBG player by name; the backend calls the PUBG Developer API to resolve that name to an account, fetch their season statistics (aggregated across all game modes into a single win rate), and list their recent matches. Selecting a match retrieves that match's own statistics — kills, damage dealt, headshot rate, survival time, and final placement — computed from PUBG's raw response by the backend, never left for the frontend to interpret, and transparently served from an Amazon S3 cache-aside layer if that match has already been looked up by anyone. From there, a player can request AI-generated insights for that match: the backend composes the match's stats and the player's season context into a single prompt containing only those aggregated numbers, sends it to Gemini, and parses the response back into a structured summary with explicit strengths, weaknesses, and recommendations. A player can also request a live population comparison for the same match — a real, computed statistic (median damage percentile for that game mode) queried on demand via Amazon Athena over every match the application has ever analyzed for anyone, giving a genuine cross-player baseline instead of a fixed, arbitrary reference point. A saved analysis can be shared publicly with one click: a "Copy Share Link" action calls a standalone Amazon API Gateway + AWS Lambda endpoint that reads the saved analysis directly out of DynamoDB and renders a read-only HTML report card, entirely independent of the Spring Boot monolith. Behind the scenes, an Amazon ECS Fargate task, triggered automatically every six hours by an EventBridge Scheduler rule, proactively re-warms a DynamoDB season-stats cache for every player anyone has analyzed, reducing the number of live PUBG API calls a repeat visit needs. The whole backend is deployed on AWS Elastic Beanstalk and served through two Amazon CloudFront distributions (one for the static frontend, one for the backend API) so that every request, in both directions, travels over HTTPS with edge caching where it is safe to cache. Every one of these steps is a single automated request/response cycle, or a fully automated scheduled job; nothing requires manual data entry, a manual AWS Console click, or manual interpretation by the user beyond typing a name and clicking a match.

### Key beneficiaries

- **Individual PUBG players**, casual through semi-competitive, who want to understand their own performance without learning to manually read and cross-reference raw statistics themselves, and without having to guess whether a given number is actually good.
- **Players trying to improve at a specific weakness** (survival time, headshot accuracy, positioning implied by damage/placement patterns) who benefit from having that weakness named explicitly, and now benchmarked against real other players rather than inferred from a spreadsheet of numbers or a fixed, unvalidated ceiling.
- **Content creators and informal coaches** who want a fast, automated first-pass performance summary for a match or a player — and a one-click, publicly shareable version of that summary — without manually reviewing raw stats or footage themselves.
- **The course examiner/assessor**, as a secondary but explicit beneficiary of this specific deliverable: the project is designed to clearly demonstrate the integration of a cloud platform (AWS, across all six required service categories), an external data API (PUBG Developer API), and an AI service (Gemini) working together in one coherent, automated pipeline, which is the core learning objective this assignment assesses.

---

## Related Work

Several existing tools already surface PUBG gameplay statistics pulled from the same official PUBG Developer API this project uses:

- **op.gg's PUBG tracker** (`pubg.op.gg`) — one of the most widely used third-party PUBG stat sites, showing per-match and season statistics, leaderboards, and match history for any searchable player. Like most tools in this category, it presents raw numbers (kills, damage, rank) without interpreting what they mean for the player's own improvement, and without comparing a specific match against any wider population — the player still has to read and compare the figures themselves.
- **PUBG's own in-game/website career statistics page** — the first-party equivalent, showing lifetime and per-season stats per game mode. Comprehensive but, again, purely presentational: numbers without analysis or comparison.
- General esports/game analytics dashboards (the same category as op.gg's trackers for other titles, or generic stat-tracking sites) — the broader pattern across competitive games is the same: aggregate the numbers, visualize them, and leave interpretation to the player.

**What this project does differently**: rather than stopping at retrieval and visualization, PUBG Insight adds an interpretation layer on top of the same class of data these tools expose — computing derived metrics itself (rather than only displaying what the API already aggregates), using an LLM (Gemini) to convert those metrics into an explicit, natural-language performance summary, strengths, weaknesses, and recommendations, and using its own accumulated usage history (via Amazon Athena) to give a match a real comparative baseline none of the tools above provide. It also treats the underlying cloud infrastructure as an object of study as much as the app itself: the assignment's requirement to demonstrate real, automated cloud service integration across Compute, Containers, Storage, Networking & Content Delivery, Database, and Analytics (Elastic Beanstalk, API Gateway, Lambda, ECS Fargate, DynamoDB, S3, CloudFront, Athena) shapes the architecture as much as the player-facing feature set does — a concern none of the tools above need to address.

---

## System Architecture

### Overview

PUBG Insight's architecture is deliberately organized around a single rule carried through every design decision on this project: **every cloud service must be genuinely, automatically invoked by the application's own runtime behavior** — a user action, a piece of backend code, or a scheduled trigger — never a one-off manual step performed only for the purposes of a demo. The only manual, one-time actions anywhere in the system are initial resource provisioning (creating a bucket, a table, a distribution) — which the rubric explicitly allows — never anything in the live request path.

The system is built from four cooperating parts:

1. A **React frontend**, served over HTTPS via Amazon CloudFront, that is the only surface a user ever interacts with directly.
2. A **Spring Boot 4.1.0 / Java 21 monolith**, deployed on AWS Elastic Beanstalk (environment `Pubg-insight-backend-env`) and fronted by its own CloudFront distribution, that owns all business logic and every external integration except the two described next.
3. A **standalone serverless share endpoint** (Amazon API Gateway + AWS Lambda) that reads shared analyses directly out of DynamoDB, entirely independent of the monolith.
4. A **standalone containerized background job** (Amazon ECS Fargate, triggered by EventBridge Scheduler) that proactively refreshes cached data the monolith reads, reducing live third-party API load.

These four parts are backed by three data/analytics services — Amazon S3, Amazon DynamoDB, and Amazon Athena — and connect to two third-party APIs — the PUBG Developer API and Google's Gemini API.

### System architecture diagram

```mermaid
flowchart TB
    User(["Player / Browser"])

    subgraph CDN["Networking & Content Delivery — Amazon CloudFront"]
        direction LR
        CFFE["CloudFront distribution\n(frontend)\nHTTPS + edge caching"]
        CFBE["CloudFront distribution\n(backend API)\nHTTPS, no-cache policy\n(fixes mixed-content HTTPS→HTTP)"]
    end

    subgraph Storage["Storage — Amazon S3"]
        direction LR
        S3FE[("S3 bucket: pubg-insight-frontend\nstatic frontend build")]
        S3CACHE[("S3 bucket: pubg-insight-match-cache\nmatches/*.json — raw match cache\nanalytics/*.json — Athena feed\nathena-results/ — query output")]
    end

    subgraph Compute["Compute — AWS Elastic Beanstalk"]
        EB["Spring Boot 4.1.0 / Java 21 monolith\nPubg-insight-backend-env"]
    end

    subgraph ShareStack["Compute (bonus) — API Gateway + Lambda"]
        direction LR
        APIGW["API Gateway HTTP API\nGET /share/{playerId}/{matchId}"]
        LAMBDA["Lambda: pubg-insight-share-analysis\n(Node.js 20.x, own IAM execution role)"]
    end

    subgraph Containers["Containers — ECS Fargate"]
        direction LR
        SCHED["EventBridge Scheduler\nevery 6 hours"]
        ECS["Fargate task\nseason-stats-cache-warmer"]
    end

    subgraph Database["Database — Amazon DynamoDB"]
        direction LR
        DDBHIST[("pubg-insight-analysis-history\nPK playerId / SK matchId")]
        DDBCACHE[("pubg-insight-season-stats-cache\n1-hour TTL read-through cache")]
    end

    subgraph Analytics["Analytics — Amazon Athena"]
        ATHENA["Athena + Glue Data Catalog\ndb pubg_insight\ntable pubg_insight_analytics"]
    end

    subgraph ThirdParty["Third-party APIs"]
        direction LR
        PUBG[("PUBG Developer API")]
        GEMINI[("Google Gemini API")]
    end

    User -->|"HTTPS"| CFFE --> S3FE
    User -->|"REST/JSON via axios"| CFBE --> EB
    User -->|'"Copy Share Link" button'| APIGW --> LAMBDA
    LAMBDA -->|"GetItem"| DDBHIST

    EB -->|"Bearer token"| PUBG
    EB -->|"API key"| GEMINI
    EB -->|"cache-aside\nGetObject/PutObject"| S3CACHE
    EB -->|"PutItem / GetItem\n(save & read history)"| DDBHIST
    EB -->|"read-through\nGetItem/PutItem, 1h TTL"| DDBCACHE
    EB -->|"PutObject\n(analytics record)"| S3CACHE
    EB -->|"StartQueryExecution\nGetQueryResults"| ATHENA
    ATHENA -->|"reads external table over"| S3CACHE

    SCHED -->|"RunTask"| ECS
    ECS -->|"Scan playerId"| DDBHIST
    ECS -->|"GET /api/players/{id}/season-stats"| CFBE
```

### Why each service was chosen, and how it is automated

**Compute — AWS Elastic Beanstalk.** The Spring Boot monolith is deployed on Elastic Beanstalk rather than a manually managed EC2 instance because it gives a stable, self-healing application URL that survives the underlying EC2 instance restarting, with deployment itself (`eb deploy` / console upload of a build artifact) as the only manual step — every request the application actually serves afterward is fully automated. This is the natural home for the bulk of the business logic: PUBG/Gemini integration, request orchestration, and every AWS SDK call the monolith itself makes.

**Compute (bonus) — API Gateway + Lambda.** The very first version of this project's brief asked to investigate background/serverless processing for reducing PUBG API load; this is where that idea was finally realized as a real, user-facing feature rather than a demo-only function. A standalone Node.js 20.x Lambda function, `pubg-insight-share-analysis`, sits behind an API Gateway HTTP API route, `GET /share/{playerId}/{matchId}`. It reads the analysis-history DynamoDB table directly using its own least-privilege IAM execution role — scoped to read that one table, independent of the Spring Boot monolith's own IAM role — and renders a public, read-only HTML "report card" for a saved analysis, with a deep link back into the live app. It is invoked automatically whenever a user clicks the real "Copy Share Link" button on the AI Insights panel; nothing about it is a console-only demo function.

**Containers — ECS Fargate.** This also traces back to the original brief's "scheduled refresh" / rate-limit-protection ask. `season-stats-cache-warmer` is a small, standalone containerized job — its own `Dockerfile`, its own ECR repository — that is not a duplicate of the main backend. It scans the analysis-history DynamoDB table for every distinct player anyone has analyzed, then calls the main backend's own `season-stats` endpoint for each one, which proactively populates the read-through DynamoDB season-stats cache. It runs as a scheduled ECS Fargate task, invoked automatically every six hours by an EventBridge Scheduler rule — verified with a real one-off task run that correctly populated the cache — with no human ever triggering an individual run in normal operation. This is a genuine rate-limit-protection feature: it reduces the number of live PUBG API calls a repeat player search needs, directly addressing the shared 10-requests-per-minute PUBG API key budget this project has had to design around from early on (see Design Decision D16 in `ARCHITECTURE.md`).

**Storage — Amazon S3.** Two buckets serve two distinct purposes. One, `pubg-insight-frontend`, hosts the compiled static frontend build, served through its own CloudFront distribution. The other, `pubg-insight-match-cache`, serves three purposes under one bucket: a `matches/` prefix holding a cache-aside copy of every raw PUBG match response ever fetched (a completed match is immutable and not player-specific, so one cached object correctly serves every player who later looks up that same match); an `analytics/` prefix holding a separate, flat, application-controlled feed of exactly the fields Athena needs (deliberately decoupled from PUBG's own nested JSON:API match shape, so the Athena schema never depends on PUBG's wire format); and an `athena-results/` prefix that Athena itself writes query output to. Every write and read against this bucket happens through the AWS SDK inside `MatchService`/`S3AnalyticsWriter`, triggered by ordinary match lookups — never a manual upload.

**Networking & Content Delivery — Amazon CloudFront (and API Gateway, described above).** The frontend was originally served as plain HTTP via S3 static website hosting. Two CloudFront distributions now sit in front of the system: one in front of the S3-hosted frontend, giving it real HTTPS and real edge caching in place of plain static hosting; and one in front of the Elastic Beanstalk backend, added specifically to resolve a mixed-content bug where a browser correctly refused to let the HTTPS frontend call a plain-HTTP backend origin. The backend-facing distribution uses a no-caching origin policy, since every response it fronts is dynamic, per-user data that must never be served stale from an edge cache. The frontend's `VITE_API_BASE_URL` now points at this CloudFront domain rather than at the Elastic Beanstalk URL directly, so every single API call the frontend makes is automatically routed through it.

**Database — Amazon DynamoDB.** Two tables serve two distinct access patterns. `pubg-insight-analysis-history` (partition key `playerId`, sort key `matchId`) stores every analysis a user explicitly chooses to save, and is read by both the Spring Boot monolith's `HistoryController` and the standalone share Lambda — a genuine case of two independent compute services sharing one data store, rather than one service's private implementation detail. `pubg-insight-season-stats-cache` is a one-hour TTL read-through cache for season statistics, written and read by the monolith on an ordinary player search and proactively re-populated by the ECS Fargate job described above. Both tables are only ever touched by application code or the SDK — table creation was the only manual, one-time step.

**Analytics — Amazon Athena.** `PlayerMapper`'s radar-scaling logic explicitly documents (Design Decision D18 in `ARCHITECTURE.md`) that its skill scores are scaled against a fixed, arbitrary ceiling "since there's no population of users to normalize against." Athena closes exactly that gap using data the application was already producing as an unintentional byproduct: an external table (manual DDL, backed by the AWS Glue Data Catalog) is defined over the S3 analytics feed described above. `AthenaAnalyticsClient` issues a real SQL query (`SELECT approx_percentile(damageDealt, 0.5) ..., count(*) ... WHERE gameMode = ?`) against this table, polling Athena's asynchronous query-execution API until the query completes, whenever a user requests it via a real endpoint, `GET /api/players/{playerId}/matches/{matchId}/population-comparison`. The result — a median damage figure and sample size for the requested game mode — is used to compute how far above or below the population median that specific match's damage output was, and is rendered in the frontend's Population Comparison panel. This is a real, live-queried statistic computed from real accumulated data, not a fabricated or hard-coded number, and it is the first genuine cross-player baseline this application has ever had.

**Third-party APIs — PUBG Developer API and Google Gemini API.** The PUBG Developer API is the sole source of gameplay data (player search, match data, season statistics); Google's Gemini API is the sole AI service, converting aggregated metrics — never raw telemetry — into natural-language coaching feedback. Both integrations are described in full under Dataset / API Description below.

### Automation and IAM

Every AWS integration above is triggered by application code, a real button click, or a scheduled EventBridge rule — never a manual Console/CLI action in the live request path, which is the rubric's explicit automation requirement. Each newly added service is also given its own scoped, least-privilege identity rather than reusing one broad role everywhere: the share Lambda's execution role is scoped to reading the analysis-history table only; the ECS task's role is scoped to scanning that same table and calling the backend's own public endpoint; the Elastic Beanstalk instance role (`aws-elasticbeanstalk-ec2-role`) is scoped to the S3 and DynamoDB resources the monolith itself touches, plus the Athena/Glue actions its new analytics query needs. This mirrors the project's existing IAM pattern rather than introducing a new one, and deliberately avoids attaching an over-broad wildcard policy anywhere a narrower one is straightforward to write.

---

## System Descriptions

Each major component and the purpose it serves (see `docs/deliverables/ARCHITECTURE.md` §2 for the full package/dependency diagram):

| Component | Purpose |
|---|---|
| **React Frontend** | The only surface a user interacts with. Renders the search UI, player/season/match cards, AI insights, the population-comparison panel, and the share action. Contains no business logic — every number it displays was computed by the backend or a serverless component; it only formats and lays it out. |
| **Spring Boot Backend** | Owns all business logic, external API integration, and the bulk of AWS integration. Organized by feature (`player/`, `match/`, `insight/`, `history/`), not by technical layer, so each feature's controller/service/mapper/DTO live together. |
| **`client/pubg`** | The only code in the system that talks to the PUBG Developer API. Owns PUBG's wire-format DTOs, authentication (Bearer token), timeouts, rate limiting, and translates PUBG's HTTP semantics (404, 429, 5xx) into this app's own exception types. |
| **`client/gemini`** | The only code that talks to Google's Gemini API. Sends a single aggregated-metrics prompt per request and parses Gemini's free-text response into a structured summary/strengths/weaknesses/recommendations shape. |
| **`client/s3`** | Owns the cache-aside match cache (`S3MatchCacheClient`) and the separate analytics feed writer (`S3AnalyticsWriter`), both against the `pubg-insight-match-cache` bucket. |
| **`client/dynamodb`** | Owns the analysis-history repository and the season-stats cache repository against their respective DynamoDB tables. |
| **`client/athena`** | Owns `AthenaAnalyticsClient`, which starts an Athena query, polls for completion, and parses the resulting median-damage/sample-size row into a `PopulationComparison`. |
| **`player` feature** | Player Search (resolves a name to a PUBG account, shard, and recent match list) and Season Stats (aggregates win rate across all game modes for the current season, cached in DynamoDB). |
| **`match` feature** | Match Analytics — given a specific match and player, extracts that player's participant stats (kills, damage, headshot rate, survival time, placement) from PUBG's match response, checking the S3 cache first; also exposes the Athena-backed population-comparison endpoint. |
| **`insight` feature** | AI Insights — composes the `player` and `match` features' already-computed metrics into a prompt, sends it to Gemini via `client/gemini`, and returns the parsed result. Depends on both sibling features rather than duplicating their data-fetching logic. |
| **`history` feature** | Analysis History — persists a computed match+insight result to DynamoDB on explicit user action, so it can be retrieved (and shared) later without re-fetching from PUBG/Gemini. |
| **`common`** | Cross-cutting concerns used by every feature: CORS configuration and centralized exception-to-HTTP-response mapping. |
| **AWS Elastic Beanstalk** | Hosts the deployed Spring Boot monolith (`Pubg-insight-backend-env`), providing a stable URL independent of the underlying EC2 instance. |
| **AWS API Gateway + Lambda** | A standalone Node.js 20.x Lambda (`pubg-insight-share-analysis`) behind an API Gateway HTTP API route (`GET /share/{playerId}/{matchId}`) — reads the analysis-history DynamoDB table directly with its own least-privilege IAM role and renders a public, read-only HTML report card for a saved analysis, entirely independent of the Spring Boot monolith. Triggered by the "Copy Share Link" button on the AI Insights panel. |
| **AWS ECS (Fargate)** | A standalone containerized job (own Dockerfile/ECR repo, `season-stats-cache-warmer/`) that scans the analysis-history DynamoDB table for known players and proactively re-warms the DynamoDB season-stats cache by calling the main backend's own endpoint — run as a scheduled Fargate task, triggered automatically every six hours by an EventBridge Scheduler rule, to cut repeat live PUBG API calls. |
| **AWS DynamoDB** | Two tables: `pubg-insight-analysis-history`, persisting every saved AI-insight analysis (read by both the monolith and the share Lambda), and `pubg-insight-season-stats-cache`, a one-hour read-through cache for season stats (read/written by the monolith, warmed by the ECS job). |
| **AWS S3** | One bucket (`pubg-insight-frontend`) hosting the static frontend site, and one bucket (`pubg-insight-match-cache`) holding the raw PUBG match-response cache, a separate flat analytics feed (the input to Athena), and an Athena query-results prefix. |
| **AWS CloudFront** | Two distributions: one in front of the S3-hosted frontend (HTTPS + edge caching in place of plain S3 website hosting), one in front of the Elastic Beanstalk backend (fixes a mixed-content issue where the HTTPS frontend could not call a plain-HTTP backend origin, using a no-caching policy since every response is dynamic). |
| **AWS Athena** | An external table (Glue Data Catalog, manual DDL) over the S3 analytics feed, queried live by `AthenaAnalyticsClient` from `GET /api/players/{playerId}/matches/{matchId}/population-comparison` — computes the median damage percentile across every match this app has ever analyzed for a given game mode, the cross-player baseline the radar-scoring logic never had (see Design Decision D18 in `ARCHITECTURE.md`). |

---

## Dataset / Data Structure / API Description

### PUBG Developer API

The primary data source. A JSON:API-formatted REST API (`https://api.pubg.com`) requiring a Bearer-token API key. This project calls four of its endpoints:

- `GET /shards/{shard}/players?filter[playerNames]={name}` — resolves a player name to an account id, shard, and a `relationships.matches` list of recent match ids (PUBG exposes roughly the last 14 days of matches this way).
- `GET /shards/{shard}/matches/{matchId}` — returns full match data: match-level attributes (map, game mode) plus an `included` array mixing multiple resource types (`roster`, `participant`, `asset`); this project reads only `participant` entries, matching on `stats.playerId`, to get that player's kills, damage, headshot kills, survival time, and placement for that match.
- `GET /shards/{shard}/seasons` — lists all seasons; used to find the one flagged `isCurrentSeason: true`.
- `GET /shards/{shard}/players/{accountId}/seasons/{seasonId}` — returns `gameModeStats`, a per-game-mode (solo/duo/squad and their FPP variants) breakdown of wins and rounds played for that season, which this project sums across all modes into a single win rate.

Full request/response shapes and how they map onto this app's own DTOs are diagrammed in `docs/deliverables/ARCHITECTURE.md` §4 (Data Mapping).

### Google Gemini API

The AI service (`https://generativelanguage.googleapis.com`), called via its `generateContent` endpoint. Input is a single text prompt containing only aggregated numbers this backend already computed (map, placement, kills, headshot rate, damage, survival time, season win rate) — never raw match telemetry, per the project's AI integration principle. Output is free-form text, which this project asks Gemini (via prompt instructions) to structure into `SUMMARY:`/`STRENGTHS:`/`WEAKNESSES:`/`RECOMMENDATIONS:` lines; the backend parses that structure, falling back to returning the raw text as the summary if Gemini doesn't follow the requested format.

### Internal data structures

The backend never exposes either external API's raw shape to the frontend. Every response is a small, flat, purpose-built DTO:

- `PlayerDto` — id, name, shardId, recentMatchIds.
- `SeasonStatsDto` — wins, roundsPlayed, winRate (aggregated).
- `MatchDto` — matchId, mapName, gameMode, kills, headshotKills, headshotRate, damageDealt, timeSurvivedSeconds, winPlace.
- `InsightDto` — summary, strengths, weaknesses, recommendations.
- `PopulationComparison` — medianDamage, sampleSize, deltaPct (this match's damage relative to the population median, as a percentage).

### AWS-facing data shapes

- **DynamoDB — analysis history** (`AnalysisHistoryItem`, partition key `playerId`, sort key `matchId`): the same match/insight fields as `MatchDto`/`InsightDto` plus a `createdAt` timestamp. Read directly by both `HistoryController` in the Spring Boot backend and the standalone `pubg-insight-share-analysis` Lambda, which unmarshals the raw DynamoDB item itself via `@aws-sdk/util-dynamodb` rather than going through the Java backend.
- **DynamoDB — season-stats cache** (`SeasonStatsCacheItem`): a one-hour TTL read-through cache keyed by player id, populated on demand by `PlayerService` and proactively re-warmed every six hours by the ECS Fargate job in `season-stats-cache-warmer/`.
- **S3 — match cache**: one JSON object per match id (`matches/{matchId}.json`), the raw PUBG match response, keyed only by match id since a completed match's data is immutable and not player-specific.
- **S3 — analytics feed**: a separate, flat, app-controlled object convention (`analytics/{matchId}-{playerId}.json`, one record per analyzed match: game mode, damage dealt, etc.) that Athena's external table reads over — deliberately kept apart from the raw PUBG-shaped match cache so the Athena schema never depends on PUBG's own wire format.
- **Athena — query result**: a two-row result set (header row + one aggregate row) containing `median_damage` and `sample_size` for the requested game mode, parsed by `AthenaAnalyticsClient` into a `PopulationComparison`.

---

## References

- PUBG Developer API documentation — https://documentation.pubg.com
- Google Gemini API documentation — https://ai.google.dev/gemini-api/docs
- JSON:API specification (the format PUBG's API follows) — https://jsonapi.org
- Spring Boot reference documentation — https://docs.spring.io/spring-boot/
- React documentation — https://react.dev
- Material UI documentation — https://mui.com/material-ui/
- AWS Elastic Beanstalk documentation — https://docs.aws.amazon.com/elasticbeanstalk/
- AWS Lambda documentation — https://docs.aws.amazon.com/lambda/
- Amazon API Gateway documentation — https://docs.aws.amazon.com/apigateway/
- Amazon ECS (Fargate) documentation — https://docs.aws.amazon.com/ecs/
- Amazon EventBridge Scheduler documentation — https://docs.aws.amazon.com/scheduler/
- Amazon DynamoDB documentation — https://docs.aws.amazon.com/dynamodb/
- Amazon S3 documentation — https://docs.aws.amazon.com/s3/
- Amazon CloudFront documentation — https://docs.aws.amazon.com/cloudfront/
- Amazon Athena documentation — https://docs.aws.amazon.com/athena/
- AWS Glue Data Catalog documentation — https://docs.aws.amazon.com/glue/latest/dg/catalog-and-crawler.html
- op.gg PUBG stats (related work example) — https://pubg.op.gg
