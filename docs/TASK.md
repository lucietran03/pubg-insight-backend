# Roadmap (Weeks 7–12)

High-level plan mapping the approved architecture (see `PROJECT_CONTEXT.md`) to the assignment timeline. Demo evaluation happens in Weeks 10–12, so everything AWS-related should be working well before Week 10.

| Weeks | Focus |
|---|---|
| 7 | ~~Fix backend bootstrap + real health check~~ — done (`ab4b85f`) |
| 7–8 | ~~PUBG API client, DTOs, Service layer (Player Search)~~ — done, verified end-to-end |
| 8–9 | Match API (Feature 2: Match Analytics) built, not yet verified running (**current**) |
| 9 | DynamoDB (analysis history) + S3 (caching) |
| 10 | Gemini AI Insights integration |
| 10–11 | Athena analytics dashboard + Elastic Beanstalk deployment (AWS Academy Learner Lab — see `PROJECT_CONTEXT.md` → AWS Environment) |
| 11–12 | Solution Architecture Document + Project Report writeup, demo prep |

Documentation deliverables (Solution Architecture Document, Project Report) are tracked here like any other task — they're worth 11.5/40 rubric points and should be drafted incrementally as each component is built, not written from scratch in Week 12.

---

# Current Sprint

## Current Goal

Verify Feature 2 (Match Analytics) actually works end-to-end, the same way Player Search was verified — code review isn't enough, it needs a real run.

---

## Completed (verified against source, not commit messages)

- Backend: `@SpringBootApplication` main class, real `HealthController` (`GET /health`), `CorsConfig` for the Vite dev origin.
- Feature-based package restructure (`player/`, `health/`, `common/`, `client/pubg/`) — see `CLAUDE.md`.
- **Feature 1 (Player Search) verified working end-to-end**: real PUBG API key configured locally, `GET /api/players/{name}` returns id/name/shardId/recentMatchIds, confirmed via the running app.
- Frontend: Player Search UI (form + result card) built and wired to the backend, PUBG-branded MUI theme applied.
- Feature 2 (Match Analytics) **built but not yet run**: `PubgApiClient.findMatchById`, raw match DTOs (`client/pubg/dto`), `match/` feature package (MatchDto, MatchMapper, MatchService, MatchController, MatchNotFoundException), frontend `MatchList` component wired into `PlayerSearch` showing clickable recent-match chips → stats card.
- Assignment proposal approved by instructor.
- Decided to deploy via the RMIT-provided AWS Academy Learner Lab, not a personal AWS account — see `PROJECT_CONTEXT.md` → AWS Environment for the constraints this implies (Lab IAM role, region, session timeouts).

---

## In Progress

- Testing Feature 2 end-to-end: run the backend, search a player with non-empty `recentMatchIds`, click a match chip, confirm real stats come back.

---

## Next

- Once Match Analytics is verified: Lambda-based processing + API Gateway exposure
- DynamoDB (analysis history) + S3 (caching)
- Confirm the Learner Lab's actual region and note it in `PROJECT_CONTEXT.md`

---

## Blockers

None.
