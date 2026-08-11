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

Deployment target is the **RMIT-provided AWS Academy Learner Lab**, not a personal AWS account — chosen deliberately to avoid real billing risk while cost per-service isn't yet known. This has practical implications for how the AWS integrations below must be built:

- **IAM**: Learner Lab does not allow creating custom IAM roles/policies — use the pre-provisioned Lab role (commonly named `LabRole` / `LabInstanceProfile`) as the execution role for Lambda, Elastic Beanstalk, etc. Don't design anything that assumes we can create our own IAM roles.
- **Region**: Learner Lab typically locks you to a single region (often `us-east-1`). Confirm the actual region once logged in and use it consistently for every service — TODO: fill in once confirmed.
- **Sessions time out**: compute resources can stop when a Lab session ends and need restarting before use (e.g. before a demo). Elastic Beanstalk mitigates the worst of this — its environment gets a stable URL (`*.elasticbeanstalk.com`) that survives the underlying EC2 instance restarting or getting a new IP between sessions, so we don't need to reconfigure anything, just restart the environment if it was stopped.
- Budget is capped by the Lab itself, so cost overruns aren't a real risk here — but avoid leaving expensive resources (e.g. Athena queries over large scans) running unnecessarily anyway, as good practice.

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

## Project Report

- **Related Work** (1 pt) — reference similar existing applications/products.
- **System Architecture** (5 pts, the largest report criterion) — one or more diagrams that clearly show: (1) the full flow from each client interface operation through the system, (2) detailed interactions between all components, (3) the function of every component. Keep this diagram in sync as AWS integrations are added — it's worth as much as three AWS services combined.
- **System Descriptions** (1 pt) — explain the purpose of each component used.
- **Dataset / Data Structure / API Description** (1 pt) — describe the PUBG API data model, Gemini inputs/outputs, and internal data structures (DynamoDB items, S3 objects, etc.).
- **References** (0.5 pt) — links/sources used during development.

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

Verified against the actual source code as of commit `ab4b85f` (backend) — updated after the earlier audit found the backend had no application code; that has since been fixed.

Actually done

- Backend: real `@SpringBootApplication` main class (`PubgInsightBackendApplication`), a real `HealthController` serving `GET /health` → `{status: "UP", message: "Backend Connected"}`, and a `CorsConfig` allowing `http://localhost:5173` (the Vite dev origin). This lines up with what the frontend's `App.tsx` already expects (it reads `res.data.message`).
- Backend package skeleton created for the intended layered architecture: `client/pubg`, `client/gemini`, `dto`, `exception`, `mapper`, `model`, `repository`, `service`, `util` — each currently holds only an empty `placeholder` class, i.e. the folders exist but contain no real logic yet.
- Frontend: Vite/React project scaffolded with folders for `api/`, `routes/`, `services/`, `contexts/`, `types/`, `components/`, `hooks/`, `layouts/`, `pages/`, `utils/` — all still empty placeholders. `App.tsx` calls `GET /health`, which should now succeed against the updated backend. Dependencies for MUI, react-router-dom, and recharts are installed but unused.
- Assignment proposal approved by instructor.

Not yet done

- PUBG API client/integration (package exists, empty)
- Any AWS integration (Elastic Beanstalk, API Gateway, Lambda, DynamoDB, S3, Athena)
- Gemini integration (package exists, empty)
- Frontend routing, pages, or components (folders exist, empty)

Note: the backend's build could not be verified end-to-end in this environment (no network access to Maven Central to resolve `spring-boot-starter-parent`), but the source itself is correct on inspection — verify with `mvn spring-boot:run` locally before assuming it boots cleanly.

Next milestone

- Confirm the backend actually boots and the frontend can reach `/health` locally, then start PUBG API integration.

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
