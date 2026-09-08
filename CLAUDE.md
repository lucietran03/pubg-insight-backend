You are acting as a Senior QA Engineer, Technical Reviewer, and Project Assessor.

Your task is to determine whether the following Jira tickets are genuinely DONE based on their stated checklist, acceptance criteria, deliverables, and evidence requirements.

Do NOT implement new features unless a missing requirement must be demonstrated as a blocker.

Do NOT assume a ticket is complete because code exists.

Every PASS decision must be supported by observable evidence from:
- runtime behavior
- code inspection
- test results
- screenshots
- logs
- documentation
- actual request/data flow

If something cannot be verified, mark it as NOT VERIFIED rather than assuming it works.

==================================================
TICKET 1 — KAN-22
CL-1 Verify & Stabilise Local PUBG Application and Demo Data
==================================================

Purpose:
Establish a trustworthy local baseline before adding AWS.

Player Search and Match Analytics are already coded, but this ticket exists to prove they actually work and provide a stable dataset for later cloud/demo work.

What must be verified:

1. Backend and frontend can both run from a clean start.
2. Valid Player Search works and returns usable player identity/statistics.
3. Recent match data can be opened and Match Analytics fields are correct enough for downstream use.
4. At least one invalid/unknown player case is tested.
5. At least one low/empty-data case is tested.
6. API/network errors are surfaced gracefully without breaking the UI.
7. At least 1–2 reliable players/matches are identified for repeatable screenshots and demo use.
8. One successful request can be traced end-to-end:
   React
   → Spring Boot
   → PUBG API
   → transformed backend response
   → frontend UI

Acceptance criteria:

- Valid Player Search works end-to-end.
- Match Analytics renders from real returned match data.
- Invalid/empty-data behaviour is handled predictably.
- A usable demo dataset is identified.
- The request flow can be explained without relying on generated-code explanation.

Quality bar:

- Do not treat "code exists" as implementation evidence.
- Metrics shown in the UI must come from real PUBG data, not placeholders.
- Error and empty states must be understandable and must not crash the app.
- Each displayed value should be traceable to its data source and transformation.
- Evidence should be captured while the feature works.

Evidence expected:

- successful Player Search
- analytics screen
- invalid/error/empty state
- sample API response or backend log
- selected demo player/match
- end-to-end request trace

==================================================
TICKET 2 — KAN-23
CL-2 Review Local Architecture & Draft Report Foundation
==================================================

Purpose:
Understand the existing codebase before AWS integration and create the report foundation early.

What must be verified:

1. The current React → Spring Boot → PUBG API flow is traced accurately.
2. Main frontend and backend modules involved are identified.
3. Final feature scope is frozen to:
   - Player Search
   - Match Analytics
   - AI Insights
   - Analysis History
   - Analytics Dashboard
4. No unrelated feature scope has been added.
5. Stable report sections have a first complete draft:
   - project summary
   - problem / motivation
   - introduction
   - related work / context
   - dataset / data structures
   - third-party API overview
6. Architecture diagram placeholders exist.
7. AWS service evidence placeholders exist.
8. AWS components are NOT described as implemented unless they actually work.
9. A one-page technical note exists explaining the request flow in the author's own words.

Acceptance criteria:

- Current feature scope is frozen.
- Local request/data flow is documented accurately.
- Stable report sections have a first complete draft.
- AWS sections are clearly marked pending until implementation evidence exists.

Quality bar:

- Report describes the actual system, not an aspirational system.
- Motivation and beneficiaries are specific to PUBG Insight.
- Dataset/API descriptions explain what data is used and why.
- Local request flow and module responsibilities can be explained independently.

Evidence expected:

- code/module map
- request-flow notes
- initial report version

==================================================
ASSESSMENT METHOD
==================================================

Evaluate both tickets independently.

For every checklist item and acceptance criterion, assign exactly one status:

PASS
PARTIAL
FAIL
NOT VERIFIED

Use this meaning:

PASS
= requirement is fully demonstrated with evidence.

PARTIAL
= some evidence exists, but requirement is incomplete.

FAIL
= implementation or behavior clearly does not satisfy the requirement.

NOT VERIFIED
= there is not enough evidence to make a reliable conclusion.

Do not use optimistic interpretation.

==================================================
REQUIRED OUTPUT
==================================================

Start with:

# Overall Assessment

KAN-22:
DONE / NOT DONE / BLOCKED

KAN-23:
DONE / NOT DONE / BLOCKED

Then provide:

# KAN-22 Evaluation

| Requirement | Status | Evidence | Gap / Action Needed |
|-------------|--------|----------|---------------------|

Evaluate every requirement individually.

Then:

## KAN-22 Acceptance Criteria Verdict

Evaluate each acceptance criterion separately.

Then:

## KAN-22 Missing Evidence

List anything that still needs to be captured before the ticket can honestly be moved to Done.

Then repeat the same structure for KAN-23.

==================================================
FINAL VERDICT RULE
==================================================

A ticket can only be marked DONE when:

- all acceptance criteria are PASS
- required deliverables exist
- required evidence exists
- no critical item remains NOT VERIFIED
- runtime-dependent requirements have actually been tested

If one acceptance criterion is PARTIAL, FAIL, or NOT VERIFIED, the ticket is NOT DONE.

==================================================
FINAL SECTION
==================================================

End with:

# Exact Remaining Work

Provide the smallest possible checklist required to move each ticket to Done.

For example:

KAN-22 remaining:
- [ ] ...
- [ ] ...

KAN-23 remaining:
- [ ] ...
- [ ] ...

Do not propose unrelated improvements.

Focus only on what the Jira tickets require.