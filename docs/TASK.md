# Roadmap (Weeks 7–12)

High-level plan mapping the approved architecture (see `PROJECT_CONTEXT.md`) to the assignment timeline. Real deadline per `docs/WBS.md`/`docs/Calendar.md`: Canvas submission 12-Sep, demo target 17/18-Sep.

| Weeks | Focus |
|---|---|
| 7 | ~~Fix backend bootstrap + real health check~~ — done |
| 7–8 | ~~PUBG API client, DTOs, Service layer (Player Search)~~ — done, verified end-to-end |
| 8–9 | ~~Match API (Feature 2: Match Analytics) + Win Rate~~ — done, verified end-to-end |
| 9 | ~~Gemini AI Insights (Feature 3)~~ — done, code complete, not yet live-run |
| current | Solution Architecture Document / Project Report prose, then DynamoDB + S3 code (see below) |
| next | AWS setup (Elastic Beanstalk, API Gateway, Lambda, DynamoDB, S3, Athena) — needs Learner Lab access, owned by the user |
| final | Solution Architecture Document + Project Report writeup, demo prep |

Documentation deliverables (Solution Architecture Document, Project Report) are tracked here like any other task — they're worth 11.5/40 rubric points and should be drafted incrementally as each component is built, not written from scratch at the end.

---

# Current Sprint

## Current Goal

Finish everything that doesn't require live AWS access (docs prose, DynamoDB/S3 code prepared-but-untested), while AWS account-level setup is a separate, user-owned track.

---

## Completed (verified against source, not commit messages)

- Backend: `@SpringBootApplication` main class, real `HealthController`, `CorsConfig` for the Vite dev origin.
- Feature-based package restructure (`player/`, `match/`, `insight/`, `health/`, `common/`, `client/pubg/`, `client/gemini/`).
- **Feature 1 (Player Search)** — verified working end-to-end with real PUBG data.
- **Feature 2 (Match Analytics)** — built, tested (unit + integration), demoed live via screenshot (player "TGLTN", 65 matches).
- **Win Rate (Season Stats)** — verified live via the same screenshot.
- **Feature 3 (AI Insights / Gemini)** — code complete: `client/gemini` (client, DTOs, config), `insight/` feature package (service composes `player`+`match`, builds a metrics-only prompt, parses Gemini's response with a tolerant fallback), frontend `AiInsights` component wired into the match detail view. Unit + integration tests added. **Not yet run against a real Gemini key** — needs a live verification pass like Player Search got.
- Local baseline QA pass: timeout/network-failure handling, server-side error logging, frontend error-message differentiation, null-safety fix, UI redesign (centered layout, stat tiles, hero win-rate number), unit + integration tests, `check.sh` + `api-test.sh`.
- `docs/ARCHITECTURE.md`: system context, component view, sequence diagrams (Player Search, Match Analytics, Season Stats, AI Insights), data mapping, error flow, 11 design decisions, planned AWS architecture.
- Demo dataset: user has identified ~10 candidate active players (starting from "TGLTN").
- Decided to deploy via the RMIT-provided AWS Academy Learner Lab, not a personal AWS account.

---

## In Progress

- Solution Architecture Document (Summary + Introduction) and Project Report (Related Work, System Descriptions, Dataset/API Description, References) — drafting prose from `ARCHITECTURE.md`'s existing technical content.

---

## Next (owner noted — most of what's left needs the user's AWS Learner Lab access)

- AI: prepare DynamoDB repository code (Analysis History) and S3 client code (match/report caching) — writable now, but can't be tested without real credentials/resources.
- User: confirm Learner Lab region; create the actual DynamoDB table, S3 bucket, Athena setup; deploy to Elastic Beanstalk; wire up API Gateway + Lambda.
- User: run `./check.sh` and `./api-test.sh` locally to get a final confirmed-clean local baseline (the one thing from the local QA pass that couldn't be verified in the AI's sandboxed environment).

---

## Blockers

None on the non-AWS track. AWS track is blocked on the user's own Learner Lab session/credentials.
