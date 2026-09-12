# Roadmap (Weeks 7–12)

High-level plan mapping the approved architecture (see `PROJECT_CONTEXT.md`) to the assignment timeline. Real deadline per `docs/decisions/WBS.md`/`docs/decisions/Calendar.md`: Canvas submission 12-Sep, demo target 17/18-Sep.

| Weeks | Focus |
|---|---|
| 7 | ~~Fix backend bootstrap + real health check~~ — done |
| 7–8 | ~~PUBG API client, DTOs, Service layer (Player Search)~~ — done, verified end-to-end |
| 8–9 | ~~Match API (Feature 2: Match Analytics) + Win Rate~~ — done, verified end-to-end |
| 9 | ~~Gemini AI Insights (Feature 3)~~ — done, code complete, not yet live-run |
| 9 | ~~Solution Architecture Document / Project Report prose~~ — drafted |
| 9 | ~~DynamoDB (Analysis History) + S3 (match cache) code~~ — written, not compiled/deployed |
| current/next | AWS setup (Elastic Beanstalk, API Gateway, Lambda, Athena; deploy/verify DynamoDB+S3) — needs AWS console access, owned by the user |
| final | Demo prep, mock demo, technical Q&A rehearsal |

Documentation deliverables (Solution Architecture Document, Project Report) are tracked here like any other task — they're worth 11.5/40 rubric points and should be drafted incrementally as each component is built, not written from scratch at the end.

---

# Current Sprint

## Current Goal

Everything that doesn't need live AWS access is now done. The remaining work is entirely on the AWS console track, which only the user can do (credentials/console access).

---

## Completed (verified against source, not commit messages)

- Backend: `@SpringBootApplication` main class, real `HealthController`, `CorsConfig` for the Vite dev origin.
- Feature-based package restructure (`player/`, `match/`, `insight/`, `history/`, `health/`, `common/`, `client/pubg/`, `client/gemini/`, `client/dynamodb/`, `client/s3/`).
- **Feature 1 (Player Search)** — verified working end-to-end with real PUBG data.
- **Feature 2 (Match Analytics)** + **Win Rate (Season Stats)** — verified live via screenshot (player "TGLTN", 65 matches). Two real bugs found via live use and fixed: a React duplicate-key warning (two sibling components keyed with the same value), and PUBG's 429 rate-limit response being mismapped to a generic 502.
- **Feature 3 (AI Insights / Gemini)** — code complete, unit + integration tested. Not yet run against a real Gemini key.
- **Feature 4 (Analysis History)** — DynamoDB code complete (`client/dynamodb`, `history/`), unit + integration tested, **compile confirmed by a real `mvn test` run**. Not deployed against real AWS.
- **S3 match-data cache** — code complete (`client/s3`, cache-aside in `MatchService`, soft-fails on any cache error so a broken cache never breaks Match Analytics), **compile confirmed**. Not deployed against real AWS.
- **First real `mvn test` run** — compile succeeded across the whole codebase (including all AWS code, written blind with no prior ability to compile). One real bug found: `MatchService` expected Spring to auto-configure a classic Jackson `ObjectMapper` bean; this Spring Boot version configures a different (Jackson 3) mapper type instead, so context startup failed, breaking 8 tests. Fixed by having `MatchService` build its own `ObjectMapper` directly instead of relying on Spring DI for it.
- 10 candidate demo players identified, updated to ones confirmed to have match data within the last 2 weeks (`src/main/resources/playername.txt`): TGLTN, hwinn, Seoul_, Leab1anc_-, DouYin-_-01, pushiket, Superdduo, Bay-j, 2cut, ixxxsH.
- PUBG API call volume reduced from 8 to ~5 calls per player search (cached `findCurrentSeasonId()`, reduced match-preview prefetch count) after live testing hit the 10 req/min rate limit after 1-2 searches.
- Frontend UI redesigned per an explicit design brief: wider layout, sectioned cards (Player Overview / Season Performance / Recent Matches), rich match previews for the most recent few matches, reduced border radius, API-status indicator.
- `api-test.sh` had its own real bug fixed (JSON id-extraction regex silently failed on spaced JSON, causing checks to be skipped while still reporting "0 failed") — now prints real status/body per call and surfaces skips as warnings.
- `docs/deliverables/ARCHITECTURE.md`: system context, component view, 6 sequence diagrams (Player Search, Match Analytics, Season Stats, AI Insights, S3 caching, DynamoDB history), data mapping, error flow, 15 design decisions, planned AWS architecture with a concrete "what's needed to turn it on" checklist, plus the Jackson/ObjectMapper and Gemini-empty-key environment gotchas for future code.
- Real bug fixed: a Gemini 403 (empty `GEMINI_API_KEY` at runtime — a local config issue, not a code bug) was being shown to the user as "PUBG service is temporarily unavailable." Root cause was the frontend's `errorMessage.ts` hardcoding a message per HTTP status instead of reading the backend's own distinct per-exception message. Fixed frontend-side (surface `error.response.data.error`); also added a Gemini-specific rate-limit exception (`GeminiRateLimitException`, mirroring the existing PUBG one).
- Homepage layout pass: wider (`xl`) container with a real top bar, standalone search bar section, grouped Player Overview identity block, an explicit 3-column Recent Matches preview grid (was leaving dead space), and a click-to-reveal list for older matches (was a wall of meaningless "Match N" pills) — backed by a new `MatchDto.createdAt` field and backend-side map-code/game-mode-code → display-label translation (`MatchMapper`) so the frontend never sees raw PUBG codes like `Baltic_Main`.
- `docs/deliverables/SOLUTION_ARCHITECTURE_DOCUMENT.md` and `docs/deliverables/PROJECT_REPORT.md` drafted.
- Repo cleanup: removed dead scaffolding (empty index files, unused assets), synced stale frontend docs with backend's maintained copies, restored `CLAUDE.md` in both repos to real ongoing instructions (had drifted into completed one-off task tickets).
- Decided to deploy via the RMIT-provided AWS Academy Learner Lab, not a personal AWS account. **Superseded (2026-09-13)**: switched to a personal AWS account (Free Tier + $100 promotional credit) — the Lab's session-expiring credentials and no-custom-IAM-role restriction became too much friction this close to deadline. See D17 in `docs/deliverables/ARCHITECTURE.md` and `docs/decisions/AWS_SETUP.md`.
- **First real run against live AWS + a real Gemini timeout** (from user-provided runtime logs): confirmed the S3 cache-aside soft-fail (D12) works exactly as designed against real infrastructure — every S3 call failed with an expired/invalid Learner Lab session token, and every one fell back to the PUBG API cleanly with no user-visible error, just a server-side `WARN` log. Separately, a real Gemini read timeout during response-body extraction surfaced a genuine bug: it threw a plain `RestClientException` that escaped both `PubgApiClient`'s and `GeminiApiClient`'s catch clause (which only caught the narrower `HttpStatusCodeException`/`ResourceAccessException` subtypes), reaching the servlet container as an uncaught 500. Fixed by widening both clients' fallback catch to `RestClientException` itself.
- UI polish pass on the homepage: retryable (not dead-end) failed-match/season-stats states, `Skeleton`-shaped loading instead of spinners, a consistent resting-border convention across all cards/rows, and mobile-responsive breakpoints for the stat-tile grid and selected-match header.
- Match list went through two more redesigns after real 429s persisted (background auto-load queue, then pagination) before the actual root cause was identified: PUBG's 10 req/min limit is a **shared, app-wide** budget (one API key), which no amount of frontend-side pacing can correctly enforce on its own (two tabs/users each pacing "safely" can still collectively exceed it). Fixed properly backend-side: `PubgRateLimiter` (new, `client/pubg`) makes every `PubgApiClient` call block until a slot is free in a shared sliding 60s window, instead of racing PUBG's own 429. The frontend's match list is now simple pagination (`PAGE_SIZE` per page, capped at `MAX_MATCHES_DISPLAYED` total) — a UX choice, not a rate-limit workaround anymore.

---

## In Progress

Re-verifying the ObjectMapper fix with another `mvn test` run — fixed by inspection, not yet re-confirmed by a second real run.

---

## Next (all user-owned — needs AWS console access)

Step-by-step, click-by-click instructions for all credentials/console steps below: **`docs/decisions/AWS_SETUP.md`**.

1. Re-run `mvn test` to confirm the ObjectMapper fix resolves all 8 previously-failing tests.
2. Create the DynamoDB table (`pubg-insight-analysis-history` by default, partition key `playerId`, sort key `matchId`) and S3 bucket (`pubg-insight-match-cache` by default, must be globally-unique — see setup guide) — one-time Console setup, allowed under the rubric. **In progress**: personal-account IAM user + access key confirmed working — table/bucket creation not yet done.
3. Verify DynamoDB/S3 actually work against real AWS (search a player, view a match, save analysis history, confirm a second lookup of the same match is a cache hit).
4. Deploy to Elastic Beanstalk, wire up API Gateway + Lambda, set up Athena — all still fully unbuilt.
5. Build Feature 5 (Analytics Dashboard) once Athena has real historical data to query.

---

## Blockers

None on the non-AWS track — it's finished. Everything remaining is blocked on the user's own AWS console access.
