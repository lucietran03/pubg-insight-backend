# Solution Architecture Document — PUBG Insight

> Submission-ready prose for the assignment's Solution Architecture Document deliverable (Summary 0.5 pt, Introduction 1 pt — see `PROJECT_CONTEXT.md` → Deliverables). Architecture diagrams referenced throughout live in `docs/ARCHITECTURE.md`; copy/adapt the sections below directly into the actual submission document.

---

## Summary

PUBG Insight is a cloud-native analytics platform that turns raw PUBG (PlayerUnknown's Battlegrounds) gameplay data into interpreted, actionable performance feedback. A React frontend lets a player search any username; a Spring Boot backend retrieves that player's profile, season statistics, and match history from the official PUBG Developer API, computes derived metrics (headshot rate, win rate, per-match placement and damage), and — on request — sends those aggregated metrics (never raw telemetry) to Google's Gemini API to generate a natural-language performance summary, strengths, weaknesses, and recommendations. The system is deployed on AWS, using Elastic Beanstalk, API Gateway, Lambda, DynamoDB, S3, and Athena to host the application, automate data processing, persist historical analyses, and power trend reporting. The result is a single, automated pipeline — from a player typing a name to receiving both hard statistics and AI-interpreted feedback — that replaces the manual, spreadsheet-style comparison players currently have to do themselves across existing stat-tracking sites.

---

## Introduction

### Motivations

Several third-party websites already expose PUBG statistics — kill counts, damage totals, survival time, win rate — pulled from the same official PUBG Developer API this project uses. What none of them do is *interpret* those numbers. A player is left to manually compare a handful of figures across matches and guess at what they mean: is a 21% headshot rate good for their rank? Is their damage output the reason they're not converting kills into wins, or is it their survival time? This project's motivation is to remove that manual interpretation step entirely, by combining three things that are individually common but rarely combined for this purpose: (1) official, authoritative gameplay data from PUBG's own API, (2) cloud-based automated data processing and persistence so a player's history accumulates and can be trended over time, and (3) an AI layer that converts the resulting aggregated numbers into the kind of plain-language "here's what's going well, here's what isn't, here's what to work on" feedback a human coach would give — without ever needing a human coach, or the player needing to learn what each raw statistic actually implies.

### What it does

At a high level, PUBG Insight is a two-repository web application (a React frontend, a Spring Boot backend) backed by AWS cloud infrastructure. A user searches for a PUBG player by name; the backend calls the PUBG Developer API to resolve that name to an account, fetch their season statistics (aggregated across all game modes into a single win rate), and list their recent matches. Selecting a match retrieves that match's own statistics — kills, damage dealt, headshot rate, survival time, and final placement — computed from PUBG's raw response by the backend, never left for the frontend to interpret. From there, a player can request AI-generated insights for that match: the backend composes the match's stats and the player's season context into a single prompt containing only those aggregated numbers, sends it to Gemini, and parses the response back into a structured summary with explicit strengths, weaknesses, and recommendations. Every one of these steps — search, match lookup, season aggregation, AI insight generation — is a single automated request/response cycle; nothing requires manual data entry or manual interpretation by the user beyond typing a name and clicking a match. The approved architecture additionally persists each analysis to DynamoDB and caches match data in S3 so that historical trends (win rate over time, damage trend, headshot trend) can be queried through Athena and visualized on a dashboard, and the whole backend is deployed on AWS Elastic Beanstalk behind API Gateway and Lambda so that the PUBG-data-retrieval step itself runs as cloud infrastructure rather than a fixed server process.

### Key beneficiaries

- **Individual PUBG players**, casual through semi-competitive, who want to understand their own performance without learning to manually read and cross-reference raw statistics themselves.
- **Players trying to improve at a specific weakness** (survival time, headshot accuracy, positioning implied by damage/placement patterns) who benefit from having that weakness named explicitly rather than inferred from a spreadsheet of numbers.
- **Content creators and informal coaches** who want a fast, automated first-pass performance summary for a match or a player, without manually reviewing raw stats or footage themselves.
- **The course examiner/assessor**, as a secondary but explicit beneficiary of this specific deliverable: the project is designed to clearly demonstrate the integration of a cloud platform (AWS), an external data API (PUBG Developer API), and an AI service (Gemini) working together in one coherent, automated pipeline, which is the core learning objective this assignment assesses.
