# PUBG Insight V2 - Product Redesign Prompt

You have full access to the current PUBG Insight frontend and backend repositories.

The current implementation technically works, but **the product experience is disappointing**. It feels like a statistics viewer with an AI summary rather than an AI-powered performance analytics platform.

I want you to completely rethink the product from a user experience perspective.

Do **NOT** simply redesign the UI. Instead, redesign the **entire information architecture, analytics system, and insight generation workflow**.

Imagine this is a commercial SaaS product competing with PUBG Mobile Career Results, OP.GG, Tracker.gg, Mobalytics, Blitz.gg, etc.

The goal is to make users think:

> "Wow, this application really understands how I play."

instead of

> "It just displayed some numbers and ChatGPT summarized them."

---

# Current Problems

The current dashboard has several major issues.

## 1. It only displays raw data.

It mostly shows:

- kills
- damage
- placement
- survival time
- win rate

These are merely API values.

There is almost no analysis.

The AI summary simply rewrites those values into sentences.

Example:

> "You achieved #1 with 14 kills."

This is not an insight.

---

## 2. No player identity

After using the application, users still don't know:

- what kind of player they are
- what their strengths are
- what they consistently do well
- what has improved over time
- what is getting worse

There is no overall player profile.

---

## 3. No comparisons

Everything is displayed independently.

The application never answers questions like:

- Is this match better than my average?
- Is my season improving?
- Am I becoming more aggressive?
- Is my aim getting better?
- Is this actually an exceptional match?

Without comparisons, numbers have no meaning.

---

## 4. The interface lacks visual storytelling.

Currently everything is:

Card

↓

Numbers

↓

More cards

↓

AI paragraph

The page has no hierarchy.

Nothing immediately catches attention.

Nothing feels premium.

---

# Vision

I want PUBG Insight to become an AI Performance Analytics Platform.

The application should have multiple analytical layers.

Each layer answers a different question.

---

# Layer 1 — Player Identity

Instead of only showing win rate and matches played, build an overall player profile.

Examples:

- Player Archetype
- Playing Style
- Strength Profile
- Performance Grade
- Overall Rating

For example:

Frontline Eliminator

Precision Hunter

Survival Specialist

Squad Anchor

Aggressive Fragger

Balanced Operator

These titles should be generated deterministically from statistics.

Gemini should only explain WHY.

---

# Layer 2 — Performance Radar

Build a radar (hexagon) chart similar to PUBG Mobile.

Possible dimensions:

Combat

Survival

Precision

Aggression

Support

Consistency

Each dimension should be calculated from multiple statistics.

Do not invent random values.

Create meaningful formulas.

Allow comparisons between:

Current Match vs Last 50 Matches

or

Current Season vs Previous Season

The radar chart should become the centerpiece of the dashboard.

---

# Layer 3 — Match Intelligence

A match page should not simply display:

14 kills

1435 damage

26 minutes

Instead, explain why this match mattered.

Example:

Compared with your recent 50 matches:

+210% kills

+180% damage

+35% survival

-4% headshot rate

Then explain:

"This victory was driven by exceptional positioning and sustained damage rather than precision shooting."

That is insight.

---

# Layer 4 — Season Intelligence

Transform the season section into a real analytics dashboard.

Include:

Win rate trend

Average damage trend

Placement trend

K/D trend

Headshot trend

Aggression trend

Consistency trend

Performance score trend

Highlight:

What improved

What declined

What remained stable

Generate conclusions automatically.

---

# Layer 5 — AI Coach

Instead of summarizing statistics, Gemini should behave like a coach.

It should answer questions such as:

Why did this match perform well?

What habits should continue?

What mistakes appear repeatedly?

Which metrics are improving?

Which metrics are declining?

What should the player focus on next?

Recommendations must reference actual metrics.

Avoid generic gaming advice.

---

# Layer 6 — Deep Insights

Explore additional insights derived from PUBG API data.

Potential examples:

Favorite map

Favorite game mode

Highest performing map

Lowest performing map

Favorite teammate

Most successful squad

Longest survival streak

Most aggressive match

Highest clutch potential

Highest damage match

Most efficient win

Weapon preferences

Playtime distribution

Heatmaps (if feasible)

Session trends

Peak performance hours

Consistency score

Top 10 conversion rate

Average survival percentile

Risk profile

Decision profile

Create as many meaningful insights as possible.

---

# Layer 7 — Visual Storytelling

The dashboard should immediately communicate:

Who this player is

How they play

How they have improved

How this match compares

What should happen next

Think beyond cards.

Use:

Charts

Progress rings

Radar charts

Trend graphs

Badges

Achievements

Performance timelines

Heat indicators

Comparisons

Sections with strong visual hierarchy.

Avoid pages that are just lists of numbers.

---

# Layer 8 — AI Report

At the end of the page, generate a comprehensive AI report.

Instead of:

"You got 14 kills."

Generate sections such as:

Performance Summary

Strengths

Weaknesses

Playstyle Analysis

Season Progress

Match Comparison

Risk Factors

Recommendations

Training Priorities

The report should feel like something written by a professional esports coach.

---

# Technical Expectations

Please redesign:

- information architecture
- UX flow
- UI hierarchy
- analytical metrics
- derived statistics
- AI prompting
- frontend components
- backend calculations

Only use insights that can be supported by PUBG API data or valid derived metrics.

Do not fabricate unavailable data.

When comparing statistics, clearly explain how each metric is calculated.

---

# Deliverables

Please provide:

1. Complete redesigned dashboard structure.

2. Wireframe of the new dashboard.

3. Component hierarchy.

4. New backend-derived metrics.

5. Formulas for every calculated score.

6. Database changes if needed.

7. API changes.

8. Frontend implementation plan.

9. Gemini prompt redesign.

10. Step-by-step implementation roadmap from the current version to this new version.

The final product should feel like a polished commercial analytics platform rather than a university assignment. Focus on creating genuine analytical value instead of simply displaying API data.