 # PROJECT CONTEXT

> This file provides the complete project context for both human developers and AI coding agents.
>
> Before making any code changes, please read this document completely.

---

# Assignment Context

This project is **Assignment 3 – AWS Cloud System Development**, COSC2980 Cloud Computing, RMIT University.

- Individual assessment, worth **40% of the final grade**.
- Evaluated **live via demo during Weeks 10, 11 and 12**. Canvas submission is only kept for record — work is not marked until submitted, but marks come from the demo.
- Project idea "PUBG Insight – AI-powered Performance Analytics Platform" was proposed by the student and **approved without conditions** by the course instructor (Dr. Ginel Dorleon) on 6 August 2026. No scope changes were requested — the originally proposed architecture below is the agreed plan.
- Planned to be implemented **incrementally across Weeks 7–12**.

The rubric (40 pts total) breaks down as:

| # | Criterion | Points |
|---|-----------|--------|
| 1 | Project idea & formulation | 2 |
| 2 | Skill development in new tools/tech | 3 |
| 3 | AWS service utilization & automation | 25 |
| 4 | Solution Architecture Doc – Summary | 0.5 |
| 5 | Solution Architecture Doc – Introduction | 1 |
| 6 | Project Report – Related Work | 1 |
| 7 | Project Report – System Architecture diagram(s) | 5 |
| 8 | Project Report – System Descriptions | 1 |
| 9 | Project Report – Dataset/data structure/API description | 1 |
| 10 | Project Report – References | 0.5 |

Criterion 3 (25 pts) dominates the grade and has hard rules — see "AWS Services" below. Criteria 4–10 (11.5 pts combined) are **written deliverables**, not code — see "Deliverables" below. Both matter; neither should be neglected in favor of the other.

---

# Project Goal

The motivation: existing PUBG stat sites show raw numbers (kills, damage, survival time, win rate) but leave interpretation to the player. This project automates that interpretation — combining cloud computing, data analytics, and AI to turn raw gameplay statistics into meaningful performance insights, without requiring the player to manually analyze anything.

This project is **NOT** an AI chatbot.

This project is **NOT** a PUBG statistics viewer.

It is a cloud analytics platform that demonstrates cloud-native architecture, third-party API integration, serverless processing, and AI-assisted analytics.

---

# Repository Structure

This project consists of **two independent repositories**.

## Backend Repository

Repository:

```
pubg-insight-backend
```

Responsibilities:

- REST API
- Business Logic
- PUBG API Integration
- Gemini API Integration
- AWS Services
- Data Processing
- Historical Analysis
- Authentication
- Deployment

Technology:

- Java 21
- Spring Boot 3
- Maven

---

## Frontend Repository

Repository:

```
pubg-insight-frontend
```

Responsibilities:

- User Interface
- API Communication
- Data Visualization
- Dashboard
- Charts
- Routing

Technology

- React
- TypeScript
- Vite
- Material UI

---

# High-Level Architecture

```
                User
                  │
                  ▼
        React Frontend
                  │
                  ▼
      Spring Boot Backend
                  │
      ┌───────────┼────────────┐
      ▼           ▼            ▼
 PUBG API     Gemini API     AWS
                              │
                  DynamoDB / S3 / Athena
                  (via API Gateway + Lambda,
                   deployed on Elastic Beanstalk)
```

Frontend NEVER communicates directly with PUBG API, Gemini API, or AWS.

All business logic must be implemented in the backend.

---

# Backend Call Flow

This is the file-level call graph inside the backend, one block per feature/package. Keep this updated as features are added — it's the working draft for the Project Report's required System Architecture diagram (5 pts, see Deliverables).

## Player Search (`player/`)

```
PlayerController.getPlayerByName(name)      [GET /api/players/{name}]
  └─ calls PlayerService.searchPlayerByName(name)
        ├─ calls client.pubg.PubgApiClient.findPlayerByName(name)
        │     ├─ builds request from client.pubg.PubgApiProperties (application.yml: pubg.api.*)
        │     ├─ calls PUBG Developer API  [GET /shards/{shard}/players?filter[playerNames]={name}]
        │     ├─ on success → returns client.pubg.dto.PubgPlayerListResponse
        │     ├─ on 404 (no match) → returns an empty PubgPlayerListResponse
        │     └─ on other HTTP errors → throws client.pubg.PubgApiException
        │
        ├─ if the response has no data → throws player.PlayerNotFoundException
        │     (caught by common.exception.GlobalExceptionHandler → 404 JSON)
        │
        └─ calls PlayerMapper.toPlayerDto(PubgPlayerData)
              └─ returns player.PlayerDto → PlayerController → JSON response to frontend

PubgApiException (thrown above) is also caught by common.exception.GlobalExceptionHandler → 502 JSON
```

## PUBG raw response shape (`client/pubg/dto/`)

```
PubgPlayerListResponse
  └─ data: List<PubgPlayerData>
        ├─ attributes: PubgPlayerAttributes (name, shardId, titleId)
        └─ relationships: PubgPlayerRelationships
              └─ matches: PubgRelationshipData
                    └─ data: List<PubgResourceIdentifier>   (used by PlayerMapper to build recentMatchIds)
```

## Match Analytics (`match/`)

Uses a `recentMatchIds` entry already returned by Player Search — the frontend passes both the player's PUBG account id and a chosen match id.

```
MatchController.getMatchStats(playerId, matchId)   [GET /api/players/{playerId}/matches/{matchId}]
  └─ calls MatchService.getMatchStatsForPlayer(matchId, playerId)
        ├─ calls client.pubg.PubgApiClient.findMatchById(matchId)
        │     ├─ calls PUBG Developer API  [GET /shards/{shard}/matches/{matchId}]
        │     ├─ on success → returns client.pubg.dto.PubgMatchResponse (data + included[])
        │     ├─ on 404 → returns null
        │     └─ on other HTTP errors → throws client.pubg.PubgApiException
        │
        ├─ if the response is null → throws match.MatchNotFoundException
        │
        ├─ filters response.included() for type == "participant", finds the one whose
        │     stats.playerId matches the requested playerId
        │     └─ if none found → throws match.MatchNotFoundException
        │           (both cases caught by common.exception.GlobalExceptionHandler → 404 JSON)
        │
        └─ calls MatchMapper.toMatchDto(matchId, PubgMatchAttributes, PubgParticipantStats)
              └─ returns match.MatchDto (kills, headshotRate, damageDealt, timeSurvivedSeconds, winPlace)
                    → MatchController → JSON response to frontend
```

PUBG's match response mixes several resource types in one flat `included` array (`roster`, `participant`, `asset`) — `PubgIncludedItem` models only the fields the `participant` type needs; other types' extra fields are ignored via `@JsonIgnoreProperties(ignoreUnknown = true)`.

## Cross-cutting (not part of the request chain above, applied globally)

```
common.config.CorsConfig        — Spring bean, applied to every request (allows the Vite dev origin)
health.HealthController         — standalone, GET /health, no dependencies
```

---

# Core Features

The application consists of five core features.

## Feature 1

Player Search

Retrieve player profile using PUBG Developer API.

---

## Feature 2

Match Analytics

Display player statistics such as

- Damage
- Kills
- Survival Time
- Placement
- Headshot Rate
- Win Rate

---

## Feature 3

AI Insights

Generate

- Performance Summary
- Strengths
- Weaknesses
- Recommendations

using Gemini API.

The AI receives processed gameplay metrics instead of raw match data — this keeps token usage low and matches the rubric's expectation that AWS/Lambda does the real data processing, not the LLM.

---

## Feature 4

Analysis History

Store historical analysis so users can revisit previous reports.

---

## Feature 5

Analytics Dashboard

Visualize

- Damage Trend
- KD Trend
- Win Rate Trend
- Headshot Trend

using historical data.

---

# AWS Environment

Deployment target is a **personal AWS account** (Free Tier + $100 promotional credit) — **superseded from the earlier RMIT AWS Academy Learner Lab decision (2026-09-13)**: the Lab's session-expiring credentials (refresh every few hours) and no-custom-IAM-role restriction became too much friction this close to deadline. See `docs/decisions/AWS_SETUP.md` for setup steps and `docs/decisions/LEARNER_LAB.md` (now historical only) for what the Lab constraints used to be. This has practical implications for how the AWS integrations below must be built:

- **IAM**: a personal account can create its own IAM users/roles freely — a dedicated `pubg-insight-dev` IAM user (programmatic access only, no console login) holds `AmazonS3FullAccess`/`AmazonDynamoDBFullAccess`/`AdministratorAccess-AWSElasticBeanstalk` (+ more as later phases need them). Service/execution roles for Lambda, Elastic Beanstalk, etc. can be freely created per-service, unlike the Lab's fixed `LabRole`.
- **Region**: kept as `us-east-1` (matches this app's existing default, `AWS_REGION:us-east-1` in `application.yml`) — no longer region-restricted like the Lab was, but no reason to change it.
- **Credentials don't expire**: the IAM user's access key is permanent — no more per-session refresh ritual. **Never commit the downloaded access-key CSV to either repo** — it's a real, permanent secret, unlike the Lab's self-expiring session token.
- **Real billing risk**: unlike the Lab (fixed budget, no card), cost overruns here are real money once the $100 credit and Free Tier limits are exhausted. Set a Billing alarm early (e.g. $20) and avoid leaving expensive resources (Elastic Beanstalk instances, large Athena scans) running unnecessarily.

---

# AWS Services

This is the **approved** service list from the project proposal. It already scores the maximum possible on rubric criterion 3 (25 pts) — see calculation below. **Do not add further AWS services** without a real product reason; extra services add cost, complexity, and demo risk for zero additional marks.

| Service | Rubric Category | Points | Purpose |
|---|---|---|---|
| Elastic Beanstalk | Compute | 6 | Deploy the Spring Boot backend |
| API Gateway | Networking & Content Delivery | 6 | Expose REST endpoints publicly |
| AWS Lambda | Compute | 6 | Retrieve/process PUBG data in the background |
| DynamoDB | Database | 3 | Store historical analysis results |
| Amazon S3 | Storage | 3 | Cache match data and generated reports |
| Amazon Athena | Analytics | 3 | Run queries for the analytics dashboard |

Raw total: 27 points, capped at criterion 3's 25-point maximum. This is already at ceiling — resist any temptation to bolt on more AWS services "for completeness."

**Automation is the grading condition, not an implementation detail.** Per the rubric: a service only counts if it is "fully implemented and automated" and "automatically invoked by your client interface operations/code/other services **other than CLI/AWS Console**." Every service above must be triggered by application code (user action → backend → AWS SDK call), never a manual setup step performed only once via the Console for the demo. A service that technically exists but is only ever touched through the AWS Console scores **0** for that line.

Also per the rubric: service scoring is **non-iterative** — e.g. Elastic Beanstalk auto-provisioning an EC2 instance doesn't earn separate EC2 marks. Don't architect around "hidden" services expecting extra credit.

---

# Third-party APIs

Exactly **two** third-party APIs are used, matching the rubric's cap ("only two would be graded" even if more are integrated).

## PUBG Developer API

Purpose

- Player Search
- Match History
- Season Statistics

## Google Gemini API

Purpose

Generate natural language insights.

Gemini should NEVER receive raw telemetry.

Only aggregated metrics should be sent.

**Do not add a third graded third-party API integration** — it would not earn additional marks and dilutes focus from the two that matter.

---

# Deliverables

Alongside the working application, the assignment requires two written artifacts. These are worth 11.5/40 points combined and must not be left until the last week.

## Solution Architecture Document

- **Summary** (0.5 pt)
- **Introduction** (1 pt) — must cover: (i) motivations behind the idea, (ii) what the system does at a high level, (iii) who the key beneficiaries are.

Draft prose for both: `docs/deliverables/SOLUTION_ARCHITECTURE_DOCUMENT.md` — copy/adapt directly into the actual submission document.

## Project Report

- **Related Work** (1 pt) — reference similar existing applications/products.
- **System Architecture** (5 pts, the largest report criterion) — one or more diagrams that clearly show: (1) the full flow from each client interface operation through the system, (2) detailed interactions between all components, (3) the function of every component. See `docs/deliverables/ARCHITECTURE.md` for the current working set of diagrams (system context, component view, per-feature sequence diagrams, data mapping, error flow) — it's the direct source material for this section. Keep it in sync as AWS integrations are added — it's worth as much as three AWS services combined.
- **System Descriptions** (1 pt) — explain the purpose of each component used.
- **Dataset / Data Structure / API Description** (1 pt) — describe the PUBG API data model, Gemini inputs/outputs, and internal data structures (DynamoDB items, S3 objects, etc.).
- **References** (0.5 pt) — links/sources used during development.

Draft prose for Related Work / System Descriptions / Dataset & API Description / References: `docs/deliverables/PROJECT_REPORT.md`.

Both documents live outside this repository (per assignment submission format) but should be treated as first-class deliverables tracked in `TASK.md` alongside code work.

---

# Timeline

Implementation proceeds incrementally across **Weeks 7–12** (see `TASK.md` for the week-by-week roadmap). Evaluation demo happens in Weeks 10–12.

---

# Backend Responsibilities

The backend owns

- Business Logic
- Data Processing
- Authentication
- External APIs
- AWS Integration

Controllers should remain thin.

Business logic belongs inside Services.

External APIs belong inside client/.

---

# Frontend Responsibilities

The frontend owns

- UI
- Routing
- State
- Visualization

The frontend should never contain business logic.

The frontend should never communicate directly with PUBG API or Gemini API.

All communication goes through the backend REST API.

---

# Coding Principles

Follow these principles.

- Keep Controllers thin.
- Keep Components small.
- Business logic belongs in Services.
- Reusable code belongs in Utilities.
- Never duplicate code.
- Prefer composition over inheritance.
- Prefer readability over clever code.

---

# Current Status

Verified against actual source code, not commit messages or prior doc claims. See `docs/deliverables/ARCHITECTURE.md` for full diagrams and design rationale, and `docs/decisions/TASK.md` for the live sprint/roadmap tracker.

Actually done

- **Feature 1 (Player Search)** — verified working end-to-end with real PUBG data.
- **Feature 2 (Match Analytics)**, including **Win Rate (Season Stats)** — built, unit + integration tested, verified live via a real screenshot (player "TGLTN", 65 matches, real stats displayed). A real production bug (React duplicate-key warning) and a real backend bug (PUBG 429 mismapped to 502) were both found via live testing and fixed.
- **Feature 3 (AI Insights / Gemini)** — code complete (client, DTOs, `insight/` feature composing Player+Match, prompt built from aggregated metrics only, tolerant response parsing), unit + integration tested. Not yet run against a real Gemini key.
- **Feature 4 (Analysis History)** — DynamoDB integration code complete (`client/dynamodb`, `history/` feature), unit + integration tested, **compile confirmed** by a real `mvn test` run. **Deployed and verified against real AWS**: `pubg-insight-analysis-history` table created (personal AWS account), a real `POST .../history` followed by `GET .../history` round-tripped a real item through it.
- S3 match-data caching (supports Feature 2 and reduces PUBG API load) — code complete (`client/s3`, wired into `MatchService` as a cache-aside layer with soft-fail on any cache error), **compile confirmed**. **Deployed and verified against real AWS**: `pubg-insight-match-cache` bucket created, a second `GET .../matches/{id}` call for the same match came back visibly faster than the first with no `S3Exception` warnings, confirming the cache-aside path is live.
- **First real `mvn compile`/`mvn test` run completed** — compile succeeded across the entire codebase (including all AWS code written without any prior ability to compile it). One real bug found and fixed: `MatchService` assumed Spring would auto-configure a classic Jackson `ObjectMapper` bean, but this Spring Boot version configures a different (Jackson 3) mapper type instead, so no such bean existed — this broke 8 tests (every test building a real `MatchService`). Fixed by having `MatchService` construct its own `ObjectMapper` directly. See `docs/deliverables/ARCHITECTURE.md` §8 for detail — this is a real environment fact future Jackson-using code needs to know about.
- Frontend: full UI for Features 1-3, PUBG-branded MUI theme, redesigned per an explicit design brief (wider layout, sectioned cards, rich match previews, reduced border radius) — see `docs/deliverables/ARCHITECTURE.md` design decisions.
- Local baseline QA: error handling for PUBG/Gemini outages, timeouts, and rate limits (429 preserved distinctly from 502/500), server-side failure logging, frontend error-message differentiation by failure type, `check.sh` (compile+test) and `api-test.sh` (live HTTP smoke test, now prints real status/body per call after a real bug in its own id-extraction regex was found and fixed).
- Real PUBG API call volume per search reduced from 8 to ~5 (cached season id, fewer auto-loaded match previews) after live testing hit the 10 req/min free-tier limit after 1-2 searches.
- Solution Architecture Document and Project Report prose drafted (`docs/deliverables/SOLUTION_ARCHITECTURE_DOCUMENT.md`, `docs/deliverables/PROJECT_REPORT.md`).
- Assignment proposal approved by instructor. AWS deployment target: personal AWS account (Free Tier + $100 promotional credit) — superseded from an earlier RMIT Learner Lab decision, see D17 in `docs/deliverables/ARCHITECTURE.md`.
- DynamoDB table (`pubg-insight-analysis-history`) and S3 bucket (`pubg-insight-match-cache`) created on the personal AWS account and verified working end-to-end against real AWS.

Not yet done

- **Feature 5 (Analytics Dashboard)** — no code yet; needs Athena set up and historical data to actually exist first.
- Elastic Beanstalk, API Gateway, Lambda, Athena — no code at all, no infrastructure created yet.
- Frontend routing — still a single page; will matter once History/Dashboard need separate views.
- Re-running `mvn test` to confirm the ObjectMapper fix actually resolves all 8 previously-failing tests (fixed by inspection, not yet re-verified by another real run).

Next milestone

- User: re-run `mvn test` to confirm the ObjectMapper fix, then deploy to Elastic Beanstalk (Single Instance preset, not Load Balanced — see cost notes), wire up API Gateway + Lambda, set up Athena.

---

# Out of Scope

Do NOT implement

- Machine Learning training
- Custom AI models
- PUBG cheats
- Real-time multiplayer
- Desktop applications
- Mobile applications
- Additional AWS services beyond the approved list (unless the user explicitly requests a change)
- A third graded third-party API integration

---

# AI Agent Instructions

Before writing code:

1. Read this file.
2. Read `TASK.md` for the current sprint and roadmap.
3. Verify any "Completed" claim against the actual source code before building on top of it — this repo's docs and commit messages have previously claimed features (a health endpoint, FE↔BE connection) that did not exist in code.
4. Preserve the existing architecture and the approved AWS service list.
5. Do not change technologies without justification.
6. Prefer incremental changes.
7. Keep commits small.
8. Explain major architectural decisions.
9. Do not introduce unnecessary dependencies.
10. Do not refactor unrelated code.
11. Remember that every AWS integration must be automated (app-triggered), not a manual Console step.

See `CLAUDE.md` for the full AI agent operating instructions.

The goal is to maintain a clean, production-like codebase throughout the assignment, while keeping the rubric's grading mechanics — automation, the approved service/API budget, and the written deliverables — in view at all times.
