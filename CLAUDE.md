# PUBG Insight — Maximize Assessment 3 Score (Cloud Services Breadth)

## Context

Reviewed the real grading spec (`docs/decisions/ASSESSMENT_3_S2-1.pdf`, `docs/decisions/Assignment 3 - AWS Cloud System Development.pdf`). The single largest criterion — **"Appropriate utilization and full implementation of cloud services" (25–32 of 40 total points)** — is scored mechanically:

- Must use AWS services across **6 categories**: Compute, Containers, Storage, Networking & Content Delivery, Database, Analytics.
- Each service must be **fully implemented and automated** — invoked by the app's own code/client operations, not just clicked into existence in the AWS Console.
- **Elastic Beanstalk, Lambda, API Gateway, ECS, EMR = 6 marks per type.**
- Every other qualifying service (S3, DynamoDB, etc.) = **3 marks per type**.
- One AWS-managed service's auto-provisioned sub-resources (e.g. EB's own EC2) don't score separately — but two genuinely distinct top-level services both score.
- Third-party APIs (already have PUBG API + Gemini) = 2 marks per type, capped at 2 types.

**Current coverage**: Compute (Elastic Beanstalk, 6), Storage (S3, 3), Database (DynamoDB, 3), third-party APIs (PUBG + Gemini, 4) = ~16 pts. **Containers, Networking & Content Delivery, and Analytics are entirely unused** — the three categories worth the most remaining points, including the highest-value bucket (Lambda, API Gateway, ECS, EMR).

Goal: close all three gaps with additions that are genuinely justified — not bolted on for marks alone — by tying each one back to a real gap already identified earlier in this project's own design work. Cost and time are not constraints; the user is prioritizing maximum score and product completeness over both.

## Planned additions (each closes one missing category, each has a real product reason)

### 1. Analytics — Amazon Athena over the existing S3 match cache

`S3MatchCacheClient` already writes every fetched match's raw JSON to S3 permanently (cache-aside). This is, unintentionally, the start of a real data lake. Earlier in this project, `PlayerMapper`'s radar-scaling code explicitly notes scores are scaled "against a fixed ceiling rather than other players' stats, since there's no population of users to normalize against" — Athena querying the accumulated match cache finally provides that missing population baseline. Concretely: a Glue table (or manual DDL) over the S3 match-cache prefix, queried on-demand by a new backend endpoint (e.g. a percentile/average comparison across all cached matches), surfaced as a small new "vs all analyzed players" insight. Must be invoked by the app (a real query triggered by a real endpoint), not just run ad hoc in the console.

### 2. Networking & Content Delivery — CloudFront in front of the S3 frontend

Currently the frontend is plain S3 static website hosting (HTTP only, no CDN). Put a CloudFront distribution in front of it: real HTTPS, real edge caching, a genuine improvement independent of the rubric. Lowest-effort of the four additions.

### 3. Compute (bonus, high-value type) + real background-processing gap — Lambda + API Gateway

The very first version of this project's brief asked to investigate "asynchronous/background processing if appropriate" for reducing PUBG API load — never built. Use API Gateway + Lambda for a genuinely new, useful feature: a shareable read-only summary of a saved analysis (backed by the already-existing DynamoDB analysis-history table) — `GET /share/{playerId}/{matchId}` served entirely by Lambda reading DynamoDB directly, independent of the Spring Boot monolith, returning a public shareable view. Must be a real endpoint the frontend actually links to (e.g. a "Share" button on a generated AI Insight), not a console-only demo function.

### 4. Containers — ECS (Fargate) for scheduled season-stats pre-fetching

Also traces back to the original brief's "scheduled refresh" / rate-limit-protection ask. A small standalone containerized job (not a duplicate of the main app) that periodically refreshes cached season stats for recently-searched players via EventBridge Scheduler → ECS Fargate task, writing into the existing DynamoDB/S3 cache layers the main app already reads from. Reduces live PUBG API calls for repeat searches — a real rate-limit-protection feature, not a redundant second copy of the backend.

## Constraints carried over from the rest of this project (unchanged)

- Do not fabricate data or invent unjustified metrics — Athena's population comparison must be a real computed statistic from real cached data.
- Do not rewrite the existing information architecture or visual design passes already completed and approved.
- Every new AWS resource needs real IAM permissions attached deliberately (same pattern as the existing `aws-elasticbeanstalk-ec2-role` S3/DynamoDB grants) — no overly broad wildcard policies where a scoped one is easy.
- Keep committing/pushing to git as work lands, same as the rest of this session.
- This is for a real graded demo in the next few days — prioritize getting each of the 4 additions genuinely working end-to-end over polishing any single one further.
