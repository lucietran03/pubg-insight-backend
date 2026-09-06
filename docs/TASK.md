# Roadmap (Weeks 7–12)

High-level plan mapping the approved architecture (see `PROJECT_CONTEXT.md`) to the assignment timeline. Real deadline per `docs/WBS.md`/`docs/Calendar.md`: Canvas submission 12-Sep, demo target 17/18-Sep.

| Weeks | Focus |
|---|---|
| 7 | ~~Fix backend bootstrap + real health check~~ — done |
| 7–8 | ~~PUBG API client, DTOs, Service layer (Player Search)~~ — done, verified end-to-end |
| 8–9 | ~~Match API (Feature 2: Match Analytics) + Win Rate~~ — done, verified end-to-end |
| 9 | ~~Gemini AI Insights (Feature 3)~~ — done, code complete, not yet live-run |
| 9 | ~~Solution Architecture Document / Project Report prose~~ — drafted |
| 9 | ~~DynamoDB (Analysis History) + S3 (match cache) code~~ — written, not compiled/deployed |
| current/next | AWS setup (Elastic Beanstalk, API Gateway, Lambda, Athena; deploy/verify DynamoDB+S3) — needs Learner Lab access, owned by the user |
| final | Demo prep, mock demo, technical Q&A rehearsal |

Documentation deliverables (Solution Architecture Document, Project Report) are tracked here like any other task — they're worth 11.5/40 rubric points and should be drafted incrementally as each component is built, not written from scratch at the end.

---

# Current Sprint

## Current Goal

Everything that doesn't need live AWS access is now done. The remaining work is entirely on the AWS Learner Lab track, which only the user can do (region/credentials/console access).

---

## Completed (verified against source, not commit messages)

- Backend: `@SpringBootApplication` main class, real `HealthController`, `CorsConfig` for the Vite dev origin.
- Feature-based package restructure (`player/`, `match/`, `insight/`, `history/`, `health/`, `common/`, `client/pubg/`, `client/gemini/`, `client/dynamodb/`, `client/s3/`).
- **Feature 1 (Player Search)** — verified working end-to-end with real PUBG data.
- **Feature 2 (Match Analytics)** + **Win Rate (Season Stats)** — verified live via screenshot (player "TGLTN", 65 matches). Two real bugs found via live use and fixed: a React duplicate-key warning (two sibling components keyed with the same value), and PUBG's 429 rate-limit response being mismapped to a generic 502.
- **Feature 3 (AI Insights / Gemini)** — code complete, unit + integration tested. Not yet run against a real Gemini key.
- **Feature 4 (Analysis History)** — DynamoDB code complete (`client/dynamodb`, `history/`), unit + integration tested. Not compiled (no network in the dev environment) or deployed.
- **S3 match-data cache** — code complete (`client/s3`, cache-aside in `MatchService`, soft-fails on any cache error so a broken cache never breaks Match Analytics). Not compiled or deployed.
- PUBG API call volume reduced from 8 to ~5 calls per player search (cached `findCurrentSeasonId()`, reduced match-preview prefetch count) after live testing hit the 10 req/min rate limit after 1-2 searches.
- Frontend UI redesigned per an explicit design brief: wider layout, sectioned cards (Player Overview / Season Performance / Recent Matches), rich match previews for the most recent few matches, reduced border radius, API-status indicator.
- `api-test.sh` had its own real bug fixed (JSON id-extraction regex silently failed on spaced JSON, causing checks to be skipped while still reporting "0 failed") — now prints real status/body per call and surfaces skips as warnings.
- `docs/ARCHITECTURE.md`: system context, component view, 6 sequence diagrams (Player Search, Match Analytics, Season Stats, AI Insights, S3 caching, DynamoDB history), data mapping, error flow, 13 design decisions, planned AWS architecture with a concrete "what's needed to turn it on" checklist.
- `docs/SOLUTION_ARCHITECTURE_DOCUMENT.md` and `docs/PROJECT_REPORT.md` drafted.
- Demo dataset: user has identified ~10 candidate active players (starting from "TGLTN").
- Decided to deploy via the RMIT-provided AWS Academy Learner Lab, not a personal AWS account.

---

## In Progress

Nothing on the non-AWS track — see Next.

---

## Next (all user-owned — needs Learner Lab access)

1. Confirm the Learner Lab's actual region (fill in the TODO in `PROJECT_CONTEXT.md`).
2. Run `mvn compile` / `mvn test` for the first time with real network access — this is the first real compilation check for the DynamoDB/S3 code (and honestly for a lot of the rest of the backend, given this project's dev environment never had network access).
3. Create the DynamoDB table (`pubg-insight-analysis-history` by default, partition key `playerId`, sort key `matchId`) and S3 bucket (`pubg-insight-match-cache` by default) — one-time Console setup, allowed under the rubric.
4. Verify DynamoDB/S3 actually work against real AWS (search a player, view a match, save analysis history, confirm a second lookup of the same match is a cache hit).
5. Deploy to Elastic Beanstalk, wire up API Gateway + Lambda, set up Athena — all still fully unbuilt.
6. Build Feature 5 (Analytics Dashboard) once Athena has real historical data to query.

---

## Blockers

None on the non-AWS track — it's finished. Everything remaining is blocked on the user's own Learner Lab session/credentials.
