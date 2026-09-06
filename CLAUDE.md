You are acting as a Senior QA Engineer, Software Architect, and Code Reviewer.

Your task is NOT to implement new features.

Your task is to verify that the existing implementation satisfies the following Jira ticket.

---

# Jira Ticket

Purpose

Establish a trustworthy local baseline before adding AWS.

Player Search and Match Analytics are already implemented.

Your responsibility is to verify that they actually work correctly and are production-ready for the next development phase.

---

## What to verify

### 1. Backend + Frontend Startup

Verify that both repositories can start successfully from a clean state.

Check for:

- startup errors
- dependency issues
- configuration issues
- missing environment variables
- API key loading
- runtime exceptions

---

### 2. Player Search

Verify that Player Search works end-to-end.

Trace the complete request flow.

React
↓

Spring Boot
↓

PUBG API
↓

Transformation
↓

Response DTO
↓

Frontend UI

Check:

- endpoint correctness
- API client implementation
- DTO mapping
- error handling
- response transformation
- frontend rendering

Confirm the displayed player information comes from the real PUBG API instead of hardcoded values.

---

### 3. Match Analytics

Verify that Match Analytics is populated from actual PUBG match data.

Identify where each displayed metric originates.

For every displayed metric explain:

- source field from PUBG API
- backend transformation
- frontend rendering location

Typical metrics include:

- Damage
- Kills
- Survival Time
- Placement
- Headshot Rate
- Win Rate

If a metric is fake, hardcoded, incomplete, or cannot be traced, report it.

---

### 4. Error Handling

Test at least these scenarios.

- invalid player
- unknown player
- player with little or no data
- PUBG API unavailable
- invalid API key
- timeout
- rate limit

Verify that:

- backend does not crash
- frontend does not crash
- meaningful error messages are shown

---

### 5. Architecture Verification

Confirm the implementation follows the intended architecture.

Frontend

↓

Backend

↓

PUBG API

The frontend must NEVER communicate directly with PUBG API.

Business logic should remain inside backend services.

Controllers should remain thin.

---

### 6. Data Transformation

Review how raw PUBG responses are transformed.

Identify:

- unnecessary mappings
- missing mappings
- duplicated transformations
- opportunities to simplify DTOs

---

### 7. Code Quality Review

Review:

- package structure
- naming
- separation of concerns
- duplicated code
- readability
- maintainability

Highlight anything that should be improved before AWS integration.

---

### 8. Demo Readiness

Recommend one or two reliable PUBG players that consistently return useful data for demonstrations.

Verify that they contain enough match history for screenshots and future analytics.

---

# Deliverables

Produce a report with the following structure.

## Overall Status

PASS / PARTIAL PASS / FAIL

---

## Checklist

Backend Startup

PASS / FAIL

Frontend Startup

PASS / FAIL

Player Search

PASS / FAIL

Match Analytics

PASS / FAIL

Architecture

PASS / FAIL

Error Handling

PASS / FAIL

Demo Dataset

PASS / FAIL

---

## Issues Found

For every issue include:

- severity
- explanation
- affected files
- recommended fix

---

## Architecture Trace

Trace one successful request.

React

↓

Controller

↓

Service

↓

PUBG API

↓

DTO Mapping

↓

Frontend

Explain every transformation.

---

## Technical Debt

List anything that should be improved before starting AWS integration.

---

## Evidence Required

For every verified feature indicate what screenshot, API response, log, or UI evidence should be captured.

---

Do NOT write new features unless necessary to fix blocking issues.

Do NOT rewrite the architecture.

Focus only on verifying that the current implementation satisfies this Jira ticket completely.