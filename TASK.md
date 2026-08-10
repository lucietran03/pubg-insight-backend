# Roadmap (Weeks 7–12)

High-level plan mapping the approved architecture (see `PROJECT_CONTEXT.md`) to the assignment timeline. Demo evaluation happens in Weeks 10–12, so everything AWS-related should be working well before Week 10.

| Weeks | Focus |
|---|---|
| 7 | ~~Fix backend bootstrap + real health check~~ — done (`ab4b85f`) |
| 7–8 | PUBG API client, DTOs, Service layer (**current**) |
| 8–9 | Lambda-based data processing + API Gateway exposure |
| 9 | DynamoDB (analysis history) + S3 (caching) |
| 10 | Gemini AI Insights integration |
| 10–11 | Athena analytics dashboard + Elastic Beanstalk deployment |
| 11–12 | Solution Architecture Document + Project Report writeup, demo prep |

Documentation deliverables (Solution Architecture Document, Project Report) are tracked here like any other task — they're worth 11.5/40 rubric points and should be drafted incrementally as each component is built, not written from scratch in Week 12.

---

# Current Sprint

## Current Goal

Integrate PUBG API (client, DTOs, service layer) into the now-real backend package skeleton.

---

## Completed (verified against source, not commit messages)

- Backend: `@SpringBootApplication` main class, real `HealthController` (`GET /health`), `CorsConfig` for the Vite dev origin — commit `ab4b85f`.
- Backend package skeleton created: `client/pubg`, `client/gemini`, `dto`, `exception`, `mapper`, `model`, `repository`, `service`, `util` (currently empty placeholders).
- Frontend repo scaffolded: Vite/React structure, `App.tsx` calls `GET /health` (should now succeed against the updated backend — verify locally).
- Assignment proposal approved by instructor.

---

## In Progress

- PUBG Client (`client/pubg` package exists, empty)

---

## Next

- Match API
- DTOs
- Service Layer

---

## Blockers

None — recommend confirming locally (`mvn spring-boot:run` + frontend dev server) that `/health` actually round-trips before building further on top of it.
