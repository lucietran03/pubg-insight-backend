You are acting as a Senior Product Designer and Senior Frontend Engineer.

You are improving the current PUBG Insight dashboard UI.

This is NOT a full redesign and NOT a backend task.

Keep the existing dark PUBG-inspired visual identity, current color palette, and general component system.

Your job is to improve:

1. Player Overview
2. Season Performance
3. AI Coach Report consistency
4. AI analysis loading experience
5. Result reveal animation

Do not build unrelated features.
Do not fake missing backend data.
Do not modify backend APIs unless a frontend bug absolutely requires it.

==================================================
1. PLAYER OVERVIEW — FIX THE LAYOUT
   ==================================================

The current Player Overview card still looks visually disconnected.

Current layout:

Avatar on the far left

Player name starts much further to the right

Metadata sits underneath

This creates a large empty gap and makes the card feel misaligned.

Current example:

PLAYER OVERVIEW

[T avatar]          TGLTN

                  STEAM · 119 matches (14 days)

This should instead feel like one compact profile block.

Preferred structure:

[T avatar]   TGLTN
STEAM
119 matches · last 14 days

Requirements:

- Group avatar, player name, platform, and match metadata tightly.
- Use consistent horizontal and vertical alignment.
- Reduce unnecessary empty space.
- Platform metadata should remain secondary.
- Do not use an oversized badge for STEAM.
- Make the card feel like a premium player profile header rather than unrelated text elements placed inside one container.
- Preserve current data and functionality.

==================================================
2. SEASON PERFORMANCE — FIX THE HIERARCHY
   ==================================================

The current archetype section still has weak hierarchy.

Current layout places:

Frontline Eliminator
Combat 100/100 · Dominant trait

on almost the same visual line.

This makes the archetype and supporting metadata compete with each other.

The intended hierarchy should be:

Frontline Eliminator

Supporting metadata:
Combat 100/100
Dominant trait

Short archetype description

Then:

25.0%

Season Win Rate
5 wins · 20 rounds

The eye should naturally move:

Archetype
↓
Why this archetype exists
↓
Hero season metric
↓
Supporting statistics

Requirements:

- Treat "Frontline Eliminator" as the hero title.
- Move "Combat 100/100", "Dominant trait", or similar descriptors into secondary metadata.
- Avoid forcing them onto the same visual line as the archetype.
- The archetype section should feel intentional and premium.
- Keep the win rate as the major numerical focal point.
- Do not add unavailable data.

==================================================
3. SECTION TITLE HIERARCHY
   ==================================================

Section titles such as:

PLAYER OVERVIEW
SEASON PERFORMANCE
PERFORMANCE BREAKDOWN
PERFORMANCE RADAR
RECENT MATCHES
AI INSIGHTS / AI COACH REPORT

should clearly communicate section boundaries.

The current yellow accent line direction is good.

Keep it, but establish a consistent section-heading component.

Each major section should use the same:

- title typography
- accent treatment
- spacing above/below
- capitalization rules

Do not let some section titles look like headings and others look like small labels.

==================================================
4. AI INSIGHTS — ADD A REAL TOP-LEVEL HEADING
   ==================================================

The AI section currently starts too quietly.

Create a clear top-level section header such as:

AI INSIGHTS

or

AI COACH REPORT

Under it, optionally add a short muted descriptor such as:

Personalized analysis based on this match and current season performance.

This should clearly introduce a new major section of the page.

The heading must be visually consistent with other major section headings.

==================================================
5. AI COACH REPORT — CURRENT CORE PROBLEM
   ==================================================

The current AI Coach Report is better than before, but the CONTENT FORMAT is inconsistent.

Currently:

- "What You Did Well" uses checklist-style items
- "What Hurt Your Performance" uses warning-style items
- "Key Coaching Advice" uses numbered steps
- "Playstyle Diagnosis" uses an italic paragraph
- "Long-Term Development" uses a paragraph plus another bullet list

Every subsection uses a different internal format.

This makes the user constantly re-learn how to read the next section.

The report should feel like one coherent coaching document.

==================================================
6. CREATE A CONSISTENT AI REPORT DESIGN SYSTEM
   ==================================================

Use only a few repeatable content patterns.

Preferred system:

A. NARRATIVE CARD

Used for:
- Overall Verdict
- Playstyle Diagnosis
- Long-Term Development

Structure:

SECTION TITLE

1 concise paragraph

Optional supporting metric/evidence row

All narrative sections should share the same visual structure.

--------------------------------------------------

B. EVIDENCE LIST

Used for:
- What You Did Well
- What Hurt Your Performance

Structure:

Icon + Insight title
Optional short evidence/context

Example:

✓ Exceptional combat output
17 kills · 1,981 damage

✓ Strong survival
27 min · significantly above season baseline

or:

! Inconsistent match performance
High peak performance but low season consistency

Strengths and weaknesses should visually mirror each other structurally.

Do not make one a simple list and the other a completely different card system.

--------------------------------------------------

C. ACTION PLAN

Used for:
- Key Coaching Advice
- Training Priorities

Use ranked actionable steps.

Example:

01
Improve mid-game positioning

WHY
Your strongest matches occur when aggression does not compromise survival.

FOCUS
Consistency / survival

02
Improve precision under pressure

WHY
Headshot performance declines relative to the seasonal baseline.

FOCUS
Crosshair placement

These should feel like actual coaching instructions.

Do not render recommendations as generic pills.

==================================================
7. LIMIT CONTENT DENSITY
   ==================================================

The AI report should not feel like an AI text dump.

A player should be able to scan the report quickly.

Prioritize information.

For example:

- Maximum 3–4 strengths
- Maximum 3 weaknesses
- Maximum 3 coaching recommendations
- Maximum 3 training priorities

If the AI response contains more items, prioritize the most useful ones in the frontend rather than showing everything with equal importance.

Avoid long walls of text.

==================================================
8. MAKE AI INSIGHTS EVIDENCE-BASED
   ==================================================

The UI should make it obvious WHY an insight exists.

Where data is already available, connect conclusions to metrics.

Example:

High Combat Output

17 kills
1,981 damage

rather than only:

"High combat output"

Likewise:

Low precision relative to baseline

13% headshot rate
48% below season average

This should help the player learn from the report.

Do not invent evidence that the current backend does not provide.

==================================================
9. AI REPORT STORY FLOW
   ==================================================

The final report should read naturally from top to bottom.

Recommended sequence:

AI INSIGHTS

1. OVERALL VERDICT
   What kind of match was this?

2. WHAT YOU DID WELL
   What worked?

3. WHAT HURT YOUR PERFORMANCE
   What limited the result?

4. KEY COACHING ADVICE
   What should you change immediately?

5. PLAYSTYLE DIAGNOSIS
   What kind of player are you?

6. LONG-TERM DEVELOPMENT
   What should you train over the next several matches / season?

The player should finish the report understanding:

- what happened
- why
- what mattered most
- what to improve next

==================================================
10. AI ANALYSIS LOADING EXPERIENCE
    ==================================================

Add a premium loading experience when the user requests AI analysis.

The current experience should NOT be:

click button
→ generic spinner
→ report appears

I want the loading sequence to feel like the system is actively analyzing performance.

Target duration:

approximately 2.5–3 seconds minimum visual sequence

Important:

Do not artificially delay actual API results unnecessarily if the API request takes longer.

If the backend finishes faster than the visual sequence, complete the visual progression naturally.

If the backend takes longer, remain in the final processing state until the response arrives.

==================================================
11. LOADING UI DIRECTION
    ==================================================

Avoid a generic circular spinner by itself.

Use a combination of:

- animated analysis indicator
- progress bar
- percentage
- rotating / animated radar or tactical graphic if appropriate
- sequential processing messages

Example stages:

Connecting to performance data...

Fetching recent match history...

Computing season baselines...

Comparing match performance...

Analyzing player archetype...

Generating coaching recommendations...

Finalizing AI report...

The messages should update sequentially.

Example visual:

ANALYZING PERFORMANCE

██████░░░░ 62%

Comparing match performance against your season baseline...

==================================================
12. LOADING ANIMATION QUALITY
    ==================================================

The animation should match the existing PUBG design language.

Use:

- dark background
- yellow accent
- subtle motion
- smooth progress transitions
- understated glow if needed
- tactical / analytical feeling

Avoid:

- playful bouncing loaders
- neon cyberpunk effects
- huge spinning icons
- distracting particles
- excessive animation

Optional strong direction:

Animate a simplified radar chart during analysis.

For example:

Combat
Precision
Aggression
Support
Consistency
Survival

Radar lines can draw progressively while stages complete.

Do not make the animation overly complex if it harms maintainability.

==================================================
13. STAGGERED RESULT REVEAL
    ==================================================

When analysis completes, do not render the entire dashboard instantly.

Use a subtle staggered reveal.

Example:

Player Overview
fade / slide in

100ms later

Season Performance

100ms later

Performance Breakdown + Radar

100ms later

Recent Matches

100ms later

AI Insights

The effect should be subtle and fast.

Approximate stagger:
80–150ms between major sections.

Also animate radar values from 0 to their final score when practical.

Do not make users wait for animations after the data is ready.

==================================================
14. ACCESSIBILITY / UX
    ==================================================

Respect:

prefers-reduced-motion

If reduced motion is enabled:

- skip stagger animation
- skip radar drawing animation
- use simple fade or immediate render
- keep progress messages usable

Loading state must remain understandable without animation.

==================================================
15. IMPORTANT SCOPE BOUNDARY
    ==================================================

Do NOT implement future V2 features that require backend work.

Specifically, do NOT build:

- fake multi-season trend charts
- Deep Insights such as favorite map / best map / teammate synergy unless backend already exposes them
- frontend loops that fetch many matches individually
- fabricated achievements
- fake historical data

Current task is UI/UX only.

Use the existing data model and API responses.

If a requested visual requires unavailable data, clearly state the dependency instead of inventing data.

==================================================
16. IMPLEMENTATION APPROACH
    ==================================================

Before coding:

1. Inspect the current components.
2. Identify which component renders:
    - Player Overview
    - Season Performance
    - AI Coach Report
    - loading state
3. Explain the exact UX problems.
4. Propose the new component hierarchy.
5. State what components can be reused.
6. State what should be refactored.

Then implement incrementally.

Do not rewrite unrelated components.

Do not change backend contracts.

Do not replace Material UI.

Avoid unnecessary new dependencies.

==================================================
FINAL ACCEPTANCE CRITERIA
==================================================

The task is complete only when:

- Player Overview no longer looks horizontally disconnected.
- Avatar, player name, platform, and match metadata form one coherent profile block.
- Frontline Eliminator is clearly the hero archetype.
- Supporting archetype metadata no longer competes with the title.
- Major section headings are visually consistent.
- AI Insights has a clear top-level heading.
- AI report subsection content follows a consistent visual system.
- Strengths and weaknesses use mirrored evidence-list layouts.
- Coaching advice and training priorities use actionable ranked steps.
- Narrative sections use one consistent narrative-card style.
- AI report is easier to scan and teaches the player something useful.
- AI analysis has a polished progress/loading experience.
- Loading messages show meaningful stages.
- Results reveal with subtle staggered animation.
- Reduced-motion accessibility is respected.
- Existing functionality remains unchanged.
- No fake backend-dependent features are introduced.

At the end, provide:

- files changed
- components changed
- component hierarchy before vs after
- loading-state implementation
- animation behavior
- accessibility handling
- any remaining backend-dependent UI opportunities that were intentionally NOT implemented