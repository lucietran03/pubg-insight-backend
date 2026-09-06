# AI Agent Instructions — pubg-insight-backend

You are an AI software engineer working on this repository.

Before making any changes, read `docs/PROJECT_CONTEXT.md` and `docs/TASK.md` — they are the living source of truth for assignment context, approved architecture, and the current sprint. Read `docs/ARCHITECTURE.md` for diagrams, data flow, and the reasoning behind every major design decision (D1-D13). This file covers the operating rules that follow from them.

Your responsibility is NOT only to generate code. It's to maintain a clean, production-like codebase that follows the approved architecture and maximizes the assignment rubric it's graded against — and to verify claims against actual source code, not prior docs or commit messages (this repo's docs have previously claimed working features that didn't exist in code).

---

# Project Overview

PUBG Insight – AI-powered Performance Analytics Platform. RMIT COSC2980 Assignment 3, individual, 40% of final grade, evaluated live via demo (Weeks 10-12). Two repositories represent one system: `pubg-insight-backend` (this repo) and `pubg-insight-frontend`. Always consider how a change affects the other repository.

---

# Current Status (verify before trusting — see docs/PROJECT_CONTEXT.md for full detail)

Built and at least partially verified: Player Search, Match Analytics + Win Rate (live-tested with real PUBG data), AI Insights via Gemini (code complete, not yet live-tested). Built but **not compiled or deployed**: DynamoDB Analysis History and S3 match caching — this project's dev environment has never had network access to Maven Central, so `mvn compile` has never actually run against this codebase. Not started: Elastic Beanstalk, API Gateway, Lambda, Athena, Analytics Dashboard (Feature 5).

---

# Backend Package Structure — feature-based, not layer-based

`com.pubginsight` is organized by **feature**, not by technical layer. Do not create top-level `controller/`, `service/`, `dto/`, `mapper/` packages for application features.

- `client/<provider>/` — external integrations (`client/pubg`, `client/gemini`, `client/dynamodb`, `client/s3`). Each owns its own wire-format DTOs, config properties, and exception type. A client never depends on a feature-specific exception or throws one — if a feature needs a different error for "not found" vs "call failed," translate it in the feature's service layer.
- `common/` — cross-cutting concerns: `common/config/` (CorsConfig), `common/exception/` (GlobalExceptionHandler).
- `<feature>/` (`player/`, `match/`, `insight/`, `history/`, and later `dashboard/`) — one package per core feature, containing that feature's own Controller, Service, DTO, Mapper, exceptions. A higher-level feature is allowed to compose lower-level ones by calling their public Service classes directly (e.g. `insight` composes `player`+`match`; `history` composes `match`+`insight`) — this is different from sibling features (`player`/`match` never depend on each other).
- A tiny standalone concern (health check) gets its own small package rather than a generic `controller/`.

When starting a new feature: DTO → External Client (if needed) → Service → Controller → Frontend API → Frontend UI. Never implement UI before the backend API is stable.

---

# AWS Services — the approved list (do not extend without asking)

Approved set, already maxes rubric criterion 3 (25/25 pts — see `docs/PROJECT_CONTEXT.md` for the point breakdown): Elastic Beanstalk (compute), API Gateway (networking), Lambda (compute), DynamoDB (database), S3 (storage), Athena (analytics). Do not introduce additional AWS services on your own initiative — extra services earn zero additional marks and add cost/complexity/demo risk. If a task seems to need a new AWS service, ask first.

## Automation is graded, manual setup is not

A service only counts if it's "fully implemented and automated" and invoked by application code — **not CLI/AWS Console**. One-time infra setup (creating a table, a bucket) via Console is fine; the *runtime behavior* the demo relies on must be code-driven. `history/HistoryController` and `MatchService`'s S3 cache-aside logic already follow this — every AWS call happens from application code, never a manual step.

## AWS environment: Learner Lab, not a personal account

Deployment target is an RMIT-provided AWS Academy Learner Lab (see `docs/PROJECT_CONTEXT.md` → AWS Environment). Use the Lab's pre-provisioned role (`LabRole`/`LabInstanceProfile`) — never design around creating custom IAM roles/policies. Expect compute to stop between sessions; Elastic Beanstalk's stable URL means this only needs a restart, not reconfiguration.

## Third-party API budget: exactly two

PUBG Developer API and Google Gemini API. Don't introduce a third graded external API.

---

# Documentation is graded too

11.5/40 rubric points come from the Solution Architecture Document (`docs/SOLUTION_ARCHITECTURE_DOCUMENT.md`) and Project Report (`docs/PROJECT_REPORT.md`), both drafted from `docs/ARCHITECTURE.md`'s technical content. When you finish an integration or component, update `docs/ARCHITECTURE.md` (diagrams + a design decision entry if a real trade-off was made) — don't just silently write code. The System Architecture diagram section alone is worth as much as three AWS services combined.

---

# Backend Responsibilities

REST APIs, business logic, external APIs, AWS integration, data processing. Business logic belongs inside Services. Controllers stay thin. External APIs belong inside `client/`.

---

# Technology Stack

Java 21, Spring Boot 4.1.0, Maven. AWS SDK v2 (`dynamodb-enhanced`, `s3`). Third-party: PUBG Developer API, Gemini API.

---

# Coding Principles

Keep code simple. Prefer readability over cleverness. Keep Controllers thin. Separate responsibilities clearly. Avoid duplicated code. Avoid unnecessary abstractions. Write self-explanatory code — comment only the non-obvious (a hidden constraint, a workaround, a deliberate trade-off), never what the code already says. Prefer composition over inheritance.

---

# Before Writing Code

1. Does this belong in the backend or frontend?
2. Is there already an existing implementation — verified by reading the actual source, not by trusting docs?
3. Does this stay within the approved AWS services and third-party API budget?
4. Will the AWS interaction be automated (code-triggered), not a manual Console step?
5. Does `docs/ARCHITECTURE.md` need a diagram/decision update alongside this change?

---

# AI Integration Principles

Gemini is an assistant, not the business logic. Backend computes gameplay metrics; Gemini only converts structured metrics into natural language. Never send raw telemetry to Gemini — see `insight/InsightService.buildPrompt` for the current pattern (only aggregated match/season numbers).

---

# Error Handling Conventions (established, follow these)

- External clients (`client/pubg`, `client/gemini`) never throw feature-specific exceptions — only their own (`PubgApiException`, `GeminiApiException`, etc.). The calling feature's service decides what a failure means.
- Distinguish real failure modes: 404 → empty/null from the client, feature throws its own `*NotFoundException` → `GlobalExceptionHandler` → 404. Rate limits (429) get their own exception (`PubgRateLimitException`) and status, never collapsed into a generic 502 — this was a real bug, fixed once already; don't reintroduce it for a new client.
- A cache is not a feature: if something is optimization-only (like the S3 match cache), its failures must be soft-failed (logged, fallback to the real source) inside the feature service, never surfaced as an API error. If something has no fallback (like a DynamoDB save), its failure is real and should propagate to a proper error response.
- Log real causes server-side (`log.error`/`log.warn` with the underlying exception) before returning a generic client-facing message — an opaque error with nothing in the logs already cost real debugging time once in this project.

---

# Out of Scope

AI model training, PUBG cheats, real-time multiplayer, mobile/desktop apps, AWS services beyond the approved list, a third graded third-party API.

---

# If You Are Unsure

Never guess. Explain assumptions, propose alternatives, ask for clarification.

---

# Response Style

Explain architectural decisions. Keep answers concise. Produce production-quality code. Avoid unnecessary dependencies and overengineering. Optimize for maintainability over short-term convenience.

## Repository Awareness

This repo is only one part of the system. For every feature, consider: does the frontend need to consume this? Will it affect AWS integration or deployment? Never make repository-local decisions that break the overall architecture.
