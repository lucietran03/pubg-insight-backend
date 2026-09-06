# Project Report — PUBG Insight

> Submission-ready prose for the Project Report deliverable sections that aren't the System Architecture diagrams themselves (those live in `docs/ARCHITECTURE.md` and are referenced, not duplicated, below). Covers Related Work (1 pt), System Descriptions (1 pt), Dataset/Data Structure/API Description (1 pt), References (0.5 pt) — see `PROJECT_CONTEXT.md` → Deliverables.

---

## Related Work

Several existing tools already surface PUBG gameplay statistics pulled from the same official PUBG Developer API this project uses:

- **op.gg's PUBG tracker** (`pubg.op.gg`) — one of the most widely used third-party PUBG stat sites, showing per-match and season statistics, leaderboards, and match history for any searchable player. Like most tools in this category, it presents raw numbers (kills, damage, rank) without interpreting what they mean for the player's own improvement — the player still has to read and compare the figures themselves.
- **PUBG's own in-game/website career statistics page** — the first-party equivalent, showing lifetime and per-season stats per game mode. Comprehensive but, again, purely presentational: numbers without analysis.
- General esports/game analytics dashboards (the same category as op.gg's trackers for other titles, or generic stat-tracking sites) — the broader pattern across competitive games is the same: aggregate the numbers, visualize them, and leave interpretation to the player.

**What this project does differently**: rather than stopping at retrieval and visualization, PUBG Insight adds an interpretation layer on top of the same class of data these tools expose — computing derived metrics itself (rather than only displaying what the API already aggregates) and using an LLM (Gemini) to convert those metrics into an explicit, natural-language performance summary, strengths, weaknesses, and recommendations. It also treats the underlying infrastructure as the object of study as much as the app itself: the assignment's requirement to demonstrate real, automated cloud service integration (Elastic Beanstalk, API Gateway, Lambda, DynamoDB, S3, Athena) shapes the architecture as much as the player-facing feature set does, which is not a concern any of the tools above need to address.

---

## System Descriptions

Each major component and the purpose it serves (see `docs/ARCHITECTURE.md` §2 for the full package/dependency diagram):

| Component | Purpose |
|---|---|
| **React Frontend** | The only surface a user interacts with. Renders the search UI, player/season/match cards, and AI insights. Contains no business logic — every number it displays was computed by the backend; it only formats and lays it out. |
| **Spring Boot Backend** | Owns all business logic, external API integration, and (per the approved architecture) AWS integration. Organized by feature (`player/`, `match/`, `insight/`), not by technical layer, so each feature's controller/service/mapper/DTO live together. |
| **`client/pubg`** | The only code in the system that talks to the PUBG Developer API. Owns PUBG's wire-format DTOs, authentication (Bearer token), timeouts, and translates PUBG's HTTP semantics (404, 429, 5xx) into this app's own exception types. |
| **`client/gemini`** | The only code that talks to Google's Gemini API. Sends a single aggregated-metrics prompt per request and parses Gemini's free-text response into a structured summary/strengths/weaknesses/recommendations shape. |
| **`player` feature** | Player Search (resolves a name to a PUBG account, shard, and recent match list) and Season Stats (aggregates win rate across all game modes for the current season). |
| **`match` feature** | Match Analytics — given a specific match and player, extracts that player's participant stats (kills, damage, headshot rate, survival time, placement) from PUBG's match response. |
| **`insight` feature** | AI Insights — composes the `player` and `match` features' already-computed metrics into a prompt, sends it to Gemini via `client/gemini`, and returns the parsed result. Depends on both sibling features rather than duplicating their data-fetching logic. |
| **`common`** | Cross-cutting concerns used by every feature: CORS configuration and centralized exception-to-HTTP-response mapping. |
| **AWS Elastic Beanstalk** *(planned)* | Hosts the deployed Spring Boot backend, providing a stable URL independent of the underlying EC2 instance. |
| **AWS API Gateway + Lambda** *(planned)* | Exposes the backend's REST operations publicly and runs PUBG data retrieval/processing as invoked cloud functions rather than a fixed always-on process. |
| **AWS DynamoDB** *(planned)* | Persists historical analysis results (Feature 4), so past analyses survive beyond a single session. |
| **AWS S3** *(planned)* | Caches PUBG match responses and generated reports, reducing repeat calls to the rate-limited PUBG API. |
| **AWS Athena** *(planned)* | Queries the historical data cached in S3 to power the Analytics Dashboard's trend charts (Feature 5). |

---

## Dataset / Data Structure / API Description

### PUBG Developer API

The primary data source. A JSON:API-formatted REST API (`https://api.pubg.com`) requiring a Bearer-token API key. This project calls four of its endpoints:

- `GET /shards/{shard}/players?filter[playerNames]={name}` — resolves a player name to an account id, shard, and a `relationships.matches` list of recent match ids (PUBG exposes roughly the last 14 days of matches this way).
- `GET /shards/{shard}/matches/{matchId}` — returns full match data: match-level attributes (map, game mode) plus an `included` array mixing multiple resource types (`roster`, `participant`, `asset`); this project reads only `participant` entries, matching on `stats.playerId`, to get that player's kills, damage, headshot kills, survival time, and placement for that match.
- `GET /shards/{shard}/seasons` — lists all seasons; used to find the one flagged `isCurrentSeason: true`.
- `GET /shards/{shard}/players/{accountId}/seasons/{seasonId}` — returns `gameModeStats`, a per-game-mode (solo/duo/squad and their FPP variants) breakdown of wins and rounds played for that season, which this project sums across all modes into a single win rate.

Full request/response shapes and how they map onto this app's own DTOs are diagrammed in `docs/ARCHITECTURE.md` §4 (Data Mapping).

### Google Gemini API

The AI service (`https://generativelanguage.googleapis.com`), called via its `generateContent` endpoint. Input is a single text prompt containing only aggregated numbers this backend already computed (map, placement, kills, headshot rate, damage, survival time, season win rate) — never raw match telemetry, per the assignment's AI integration principle. Output is free-form text, which this project asks Gemini (via prompt instructions) to structure into `SUMMARY:`/`STRENGTHS:`/`WEAKNESSES:`/`RECOMMENDATIONS:` lines; the backend parses that structure, falling back to returning the raw text as the summary if Gemini doesn't follow the requested format.

### Internal data structures

The backend never exposes either external API's raw shape to the frontend. Every response is a small, flat, purpose-built DTO:

- `PlayerDto` — id, name, shardId, recentMatchIds.
- `SeasonStatsDto` — wins, roundsPlayed, winRate (aggregated).
- `MatchDto` — matchId, mapName, gameMode, kills, headshotKills, headshotRate, damageDealt, timeSurvivedSeconds, winPlace.
- `InsightDto` — summary, strengths, weaknesses, recommendations.

**Planned** (not yet implemented — see `docs/ARCHITECTURE.md` §7): a DynamoDB item shape for stored analysis history (player id, match id, computed metrics, timestamp) and an S3 object convention for cached PUBG match responses, to be defined when Features 4/5 are built.

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
- Amazon DynamoDB documentation — https://docs.aws.amazon.com/dynamodb/
- Amazon S3 documentation — https://docs.aws.amazon.com/s3/
- Amazon Athena documentation — https://docs.aws.amazon.com/athena/
- op.gg PUBG stats (related work example) — https://pubg.op.gg
