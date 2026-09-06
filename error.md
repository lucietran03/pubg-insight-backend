You are acting as a Senior Backend Engineer and API Reliability Reviewer.

Your task is to investigate and fix the current PUBG API rate-limit error handling in the backend.

Do NOT add new features.
Do NOT redesign unrelated code.
Focus only on correctly handling upstream PUBG API rate limits and exposing them clearly to the frontend.

---

# Current Problem

The frontend calls:

GET /api/players/{playerName}

The Spring Boot backend then calls the official PUBG API.

When multiple player searches are made within a short period, the PUBG API responds with:

429 Too Many Requests

Current backend logs show:

org.springframework.web.client.HttpClientErrorException$TooManyRequests: 429 Too Many Requests

However, the frontend receives:

502 Bad Gateway

This means the backend is currently converting the real upstream 429 response into a generic 502.

That is misleading because:

- the backend itself is not unavailable
- the upstream PUBG API is rate-limiting the request
- the frontend cannot distinguish a rate-limit problem from a generic upstream failure

---

# Current Request Flow

React Frontend
↓
Spring Boot Backend
↓
PUBG API
↓
429 Too Many Requests
↓
PubgApiException
↓
GlobalExceptionHandler
↓
502 Bad Gateway
↓
Frontend

The desired behavior is:

React Frontend
↓
Spring Boot Backend
↓
PUBG API
↓
429 Too Many Requests
↓
Backend preserves the rate-limit meaning
↓
429 Too Many Requests
↓
Frontend shows a clear retry message

---

# Goal

Fix the backend so that PUBG API rate-limit errors are handled explicitly and surfaced correctly.

The frontend should be able to distinguish:

- 404 / player not found
- 429 / rate limited
- 502 / genuine upstream failure
- 500 / unexpected backend failure

---

# What to Inspect

Review at minimum:

- PubgApiClient.java
- PubgApiException.java
- GlobalExceptionHandler.java
- any custom error response DTO
- PlayerService.java if relevant
- frontend API error handling if present

Do not assume the current exception hierarchy is correct.

---

# Required Backend Behavior

## 1. Preserve 429

If PUBG API returns 429, the backend should return HTTP 429 to the frontend.

Do not map it to 502.

---

## 2. Preserve useful metadata

If the PUBG API response contains a Retry-After header, preserve it when practical.

Examples:

- return Retry-After to the frontend
- include retry information in the error response

If no Retry-After header is present, do not invent one.

---

## 3. Produce a clear error response

Use the existing project error-response style if one already exists.

A rate-limit response should communicate something equivalent to:

{
"status": 429,
"error": "Too Many Requests",
"message": "PUBG API rate limit reached. Please try again later."
}

Do not expose internal stack traces to the frontend.

---

## 4. Distinguish upstream errors

Use appropriate mappings.

Suggested behavior:

PUBG 404
→ backend 404

PUBG 429
→ backend 429

PUBG 5xx
→ backend 502 Bad Gateway

Unexpected internal backend failure
→ backend 500

Do not blindly map every PubgApiException to one status.

---

## 5. No automatic aggressive retry

Check whether the backend or frontend automatically retries failed requests.

If retries exist, ensure 429 does not immediately trigger repeated requests that worsen the rate limit.

Do not introduce retry loops.

---

# Frontend Behavior

If the frontend already has error handling, verify that HTTP 429 is handled explicitly.

The UI should show a message similar to:

"PUBG API rate limit reached. Please try again shortly."

The application must not crash.

Do not redesign the UI.

Only improve the error state if required.

---

# Acceptance Criteria

The fix is complete only if all of the following are true:

1. A normal valid player search still works.
2. A PUBG API 429 is returned to the frontend as 429, not 502.
3. The frontend shows a meaningful rate-limit message.
4. The application does not crash.
5. No aggressive retry loop occurs.
6. Existing 404 and 5xx handling still behaves correctly.
7. The implementation remains consistent with the current architecture.

---

# Testing

Add or update tests where appropriate.

At minimum verify:

- upstream 429 → backend 429
- upstream 404 → backend 404
- upstream 5xx → backend 502

If Retry-After is supported in the current architecture, verify it too.

Do not over-test unrelated code.

---

# Deliverable

Before changing code, explain:

1. Why the frontend currently sees 502.
2. Where the 429 meaning is lost.
3. Which files need to change.
4. The minimal fix you plan to make.

Then implement the fix.

After implementation, provide:

## Root Cause

A concise explanation.

## Changes Made

List each modified file and why.

## Final Error Mapping

Show the final HTTP mapping table.

## Verification

Explain how to manually test the 429 case and what result should be observed.

---

Important constraints:

Do not treat "the code compiles" as sufficient verification.

Do not change unrelated architecture.

Do not add new AWS services or unrelated dependencies.

Prefer the smallest correct fix that preserves the real semantics of the upstream PUBG API.