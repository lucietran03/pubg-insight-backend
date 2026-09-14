I want you to perform a MAJOR PRODUCT-DESIGN PASS on the current PUBG Insight frontend.

This is NOT a request to simply polish spacing, borders, card radii, or colors.

The current dashboard is technically clean, but it is visually boring, flat, overly container-driven, and does not communicate the richness of the analytics available from the backend.

I want you to rethink how the information is presented.

Use the installed frontend/product-design skills where appropriate, but DO NOT blindly follow their default visual output.

==================================================
0. FIRST: DO NOT CODE YET
   ==================================================

Before changing any code:

1. Inspect the CURRENT frontend implementation.
2. Inspect the backend DTOs/endpoints/data models used by the frontend.
3. Identify all analytics currently available.
4. Identify analytics available from the backend but poorly surfaced or not surfaced at all.
5. Audit the screenshots/current UI against those capabilities.
6. Produce a concise redesign plan.

Create a mapping:

BACKEND DATA
→ FRONTEND TYPE
→ CURRENTLY DISPLAYED?
→ CURRENT PRESENTATION
→ PROBLEM
→ BETTER VISUAL PATTERN

Do not invent data.

Do not create fake historical data.

Do not add backend-dependent features unless the required data actually exists.

Only after this audit should you implement the redesign.

==================================================
1. THE CORE PROBLEM
   ==================================================

The current UI feels like:

section
→ dark rectangle
→ smaller dark rectangles
→ number
→ label

repeated over and over.

That is NOT enough.

The interface currently looks like a generic admin/dashboard template with PUBG colors applied to it.

I want PUBG Insight to feel like an actual PLAYER INTELLIGENCE PRODUCT.

The experience should make the user feel:

"This system understands how I play."

not:

"This website displays my API statistics."

Every major section must answer a player question.

Examples:

WHO AM I?
→ Player identity / playstyle

HOW AM I PERFORMING?
→ Season performance

WHAT AM I GOOD/BAD AT?
→ Performance profile

HOW HAVE I BEEN PLAYING RECENTLY?
→ Recent form

WHAT HAPPENED IN THIS MATCH?
→ Match diagnosis

HOW DIFFERENT WAS THIS MATCH FROM NORMAL?
→ Match vs baseline

WHY DID THIS HAPPEN?
→ AI interpretation

WHAT SHOULD I DO NEXT?
→ Coaching actions

==================================================
2. VISUAL DIRECTION
   ==================================================

Keep the current dark PUBG-inspired foundation, but make the product significantly more distinctive.

Desired identity:

TACTICAL GAMING ANALYTICS
×
PLAYER DOSSIER
×
COLLECTIBLE PROFILE
×
SUBTLE CAT / DOLL-LIKE PERSONALITY

Important:

"cat/doll personality" does NOT mean turning the website pink, childish, kawaii, or decorative for no reason.

It should appear through subtle product personality:

- collectible-profile feeling
- playful geometry
- charming micro-details
- small mascot-like moments
- achievement/badge language
- tactile UI elements
- interesting loading states
- small visual surprises
- softer shapes selectively contrasted against tactical structures

The analytics must remain credible and readable.

PUBG yellow/orange remains the primary accent.

Do NOT simply make every corner rounder.

==================================================
3. BREAK THE "CARD INSIDE CARD" HABIT
   ==================================================

This is extremely important.

DO NOT default every piece of information to:

<Card>
  <Typography>number</Typography>
  <Typography>label</Typography>
</Card>

Cards are allowed, but they must not be the only visual language.

Use different information-design patterns based on the data:

hero metric
radar
comparison
rank
progress
distribution
timeline
performance strip
badge
status
annotation
callout
mini visualization
evidence block
coach action
relationship
metric cluster
visual hierarchy

Use whitespace and typography to group information when another container is unnecessary.

Some information can exist directly on a section surface.

Reduce unnecessary nested rectangles.

==================================================
4. REDESIGN PLAYER OVERVIEW
   ==================================================

CURRENT PROBLEM:

The Player Overview contains:

avatar
name
platform
match count

and then a massive amount of dead space.

This is not acceptable.

Turn this area into a PLAYER DOSSIER.

The player identity should be visually dominant.

Consider using existing available data to create a compact identity composition such as:

avatar / monogram
player name
platform
matches analyzed
playstyle/archetype
dominant trait
recent-form signal
one or two meaningful micro metrics

Do NOT simply add more text.

Create visual hierarchy.

The section should feel like opening a player's profile card.

If archetype/playstyle data already exists, this is probably where it belongs.

==================================================
5. REDESIGN SEASON PERFORMANCE
   ==================================================

CURRENT PROBLEM:

Season Performance currently mixes:

Frontline Eliminator
Combat 100/100
Dominant Trait
description
21.7% Win Rate

without a strong conceptual hierarchy.

Archetype identity and season performance are different concepts.

Separate them visually and conceptually.

The season module should answer:

"How is my current season going?"

Win Rate may remain a hero metric if appropriate, but it should be surrounded by meaningful context.

Use available values such as:

wins
rounds
comparison
combat score
placement performance
or other existing season statistics

ONLY if the backend already provides them.

Do not create another grid of stat cards.

==================================================
6. PERFORMANCE BREAKDOWN NEEDS A REAL HIERARCHY
   ==================================================

CURRENT PROBLEM:

The current implementation is essentially:

7 equally weighted metric boxes
+
radar chart

This is usable but flat.

Not every metric is equally important.

Create hierarchy.

Potential structure:

PRIMARY PERFORMANCE SIGNALS
→ 2–3 visually dominant metrics

SUPPORTING SIGNALS
→ smaller secondary metrics

PLAYER PROFILE
→ radar visualization

The radar chart should feel like part of the player's identity/profile, not a chart randomly placed beside cards.

Make the relationship between the metrics and radar clearer.

Improve the radar's visual presence without making it oversized.

==================================================
7. CHECK METRIC SEMANTICS
   ==================================================

Audit every displayed metric.

Example:

"Finish Rate 124%"

A user can reasonably interpret a "rate" as something bounded by 100%.

If this value is actually:

an index
relative performance score
normalized value
derived metric
comparison ratio

then label and present it accurately.

Do not silently display mathematically confusing metrics.

Inspect the backend calculation before changing the label.

==================================================
8. RECENT MATCHES MUST COMMUNICATE PERFORMANCE
   ==================================================

CURRENT PROBLEM:

Recent Matches is functional but visually flat.

A match with:

#1
10 kills
1379 damage

currently feels almost identical to:

#29
1 kill
185 damage

unless it is selected.

That is bad information design.

The user should be able to scan the match history and immediately notice:

excellent matches
poor matches
high-kill matches
high-damage matches
wins/top placements
unusual performances

Create restrained performance encoding.

Possible tools:

placement emphasis
performance intensity
small accent indicators
mini performance bars
rank treatment
relative performance marker
tiny badges
micro visualization

DO NOT turn every match into a rainbow.

Use the existing palette intelligently.

Selection state must remain clearly different from performance state.

==================================================
9. RECENT MATCHES DESCRIPTION
   ==================================================

The current explanatory paragraph:

"Match data loads live from PUBG's own API..."

is too operational/developer-facing and visually interrupts the dashboard.

Do not make API limitations a dominant part of the player experience.

Move this information to:

a subtle info icon,
tooltip,
small helper text,
or another unobtrusive treatment.

The user primarily cares about their matches.

==================================================
10. SIDEBAR
    ==================================================

CURRENT PROBLEM:

"Players searched"
"Insights generated"
"Recently analyzed"

currently feels like an admin dashboard.

The sidebar should behave more like a PLAYER ANALYSIS WORKSPACE.

Prioritize:

search
current player
recently analyzed players
analysis history
navigation/context

Do not add fake history if persistence does not exist.

If history currently exists only client-side/session-side, represent that honestly.

The sidebar should support the user's workflow rather than show vanity counters.

==================================================
11. SELECTED MATCH EXPERIENCE
    ==================================================

The selected match must feel like opening a deeper layer of analysis.

Do not make it simply another card appearing underneath Recent Matches.

Create a deliberate transition:

RECENT MATCHES
→ SELECT MATCH
→ MATCH ANALYSIS

The selected match header should clearly establish:

placement
map
mode
date/time
kills
damage
survival

based on available data.

Then lead naturally into comparison and AI analysis.

==================================================
12. MATCH VS SEASON BASELINE
    ==================================================

Comparison must be visually immediate.

The player should instantly understand:

BETTER THAN NORMAL
WORSE THAN NORMAL
ABOUT NORMAL

without reading full sentences.

Delta values should visually dominate.

Example hierarchy:

+149%
large / strong

DAMAGE
medium

vs season average
small / muted

Use positive/negative treatment consistently.

Do not turn these into long sentence pills.

==================================================
13. AI INSIGHTS MUST FEEL LIKE A COACH
    ==================================================

The main section title MUST be:

AI INSIGHTS

The content should behave like a coaching report, not raw Gemini output.

Desired information journey:

OVERALL VERDICT
↓
WHAT YOU DID WELL
↓
WHAT HURT YOUR PERFORMANCE
↓
WHY IT HAPPENED
↓
WHAT TO DO NEXT
↓
LONG-TERM DEVELOPMENT

The user should understand the important lesson within 5–10 seconds.

Do not dump paragraphs.

Do not create eight identical text cards.

==================================================
14. FIX AI INSIGHT TEXT CONSISTENCY
    ==================================================

There is currently inconsistent capitalization/formatting in generated AI content.

For example, one output may render:

"Excellent Flanking"

while another becomes:

"excellent flanking"

or only the first item gets different capitalization.

THIS MUST BE FIXED AT THE PRESENTATION LAYER.

Generated text formatting must NOT depend on which array position it occupies.

Audit any logic involving:

:first-child
:first-of-type
index === 0
text-transform
capitalize
uppercase
Typography variants
CSS selectors

Create explicit typography rules.

SECTION TITLES:
consistent uppercase if that is the design convention.

ITEM TITLES:
consistent Title Case or sentence case across ALL items.

BODY COPY:
consistent sentence case.

METRIC LABELS:
consistent convention.

Do not use CSS text-transform: capitalize to blindly transform AI-generated sentences.

Normalize display intentionally.

The second/third/fourth item must use the SAME component and typography rules as the first item.

==================================================
15. ALIGNMENT CONSISTENCY
    ==================================================

Audit every section for alignment.

Do not randomly center some AI content and left-align others.

Define an alignment system.

Recommended:

Section heading → left
Insight heading → left
Body explanation → left
Metric hero → may be centered when justified
Radar → centered inside visualization area
Coach actions → left

If something is centered, there must be a design reason.

==================================================
16. CREATE REUSABLE CONTENT PRIMITIVES
    ==================================================

Do not manually style every AI output separately.

Create/reuse coherent primitives where appropriate, for example:

InsightSection
InsightItem
MetricHero
DeltaMetric
CoachAction
PerformanceSignal
MatchPerformanceIndicator
SectionHeader

Names can differ.

The important part is that repeated semantic content uses the SAME typography and spacing rules.

==================================================
17. AI COACH VISUAL SYSTEM
    ==================================================

Do not make:

WHAT YOU DID WELL
WHAT HURT YOUR PERFORMANCE
KEY COACHING ADVICE
PLAYSTYLE
LONG TERM DEVELOPMENT

look like unrelated components built by five designers.

They should belong to ONE report system.

Variation should come from semantic meaning, not random formatting.

For example:

positive evidence → one treatment
negative evidence → one treatment
diagnosis → neutral analytical treatment
actions → numbered/prioritized treatment
development → progression treatment

Maintain consistent:

heading scale
item-title scale
body scale
spacing
alignment
icon sizing

==================================================
18. LOADING EXPERIENCE
    ==================================================

AI generation should feel like analysis is happening.

Replace a generic spinner/static loading message with a staged analysis state.

Approximately:

Reading match telemetry…
Comparing against season baseline…
Evaluating combat and survival patterns…
Building coaching recommendations…
Finalizing AI insights…

Use a progress indicator and subtle motion.

Do NOT fake backend completion.

Progress may animate while the real request is pending.

If the request completes quickly, a short minimum presentation time may be used only for visual continuity.

==================================================
19. VISUAL STORYTELLING
    ==================================================

The page should have rhythm.

Not:

RECTANGLE
RECTANGLE
RECTANGLE
RECTANGLE
RECTANGLE

Think in visual chapters:

PLAYER
↓
SEASON
↓
PERFORMANCE PROFILE
↓
RECENT FORM
↓
MATCH
↓
DIAGNOSIS
↓
COACHING

Use scale, whitespace, density, dividers, composition and visualization to create these chapters.

==================================================
20. MICRO-INTERACTIONS
    ==================================================

Add subtle, purposeful interaction where appropriate:

hover response on matches
selected-match transition
metric reveal
radar entrance
delta animation
AI analysis progress
small badge feedback

Keep animations fast and restrained.

Do not make the dashboard feel like a marketing landing page.

==================================================
21. RESPONSIVENESS
    ==================================================

Desktop is important because this is an analytics dashboard, but preserve responsive behavior.

Do not solve desktop composition by hardcoding dimensions that break smaller screens.

Test at least:

large desktop
normal laptop
tablet-ish width
mobile

==================================================
22. DO NOT DESTROY WORKING FUNCTIONALITY
    ==================================================

Preserve:

player search
API integration
season stats loading
recent matches
pagination
selected match
match analysis
AI insight generation
error handling
retry behavior

Do not rewrite backend contracts just to make frontend design easier.

==================================================
23. DO NOT BUILD THESE YET WITHOUT DATA
    ==================================================

Do NOT fabricate:

multi-season trend charts
long historical timelines
favorite-map analytics
deep aggregate insights
body-hit visualizations
weapon analytics

unless the backend CURRENTLY provides sufficient real data.

If a valuable visualization cannot currently be supported, list it separately under:

FUTURE DATA-DEPENDENT OPPORTUNITIES

Do not implement fake placeholders pretending to be real analytics.

==================================================
24. DESIGN QUALITY TEST
    ==================================================

Before considering the redesign finished, ask:

Can I identify the player's identity in 2 seconds?

Can I identify their strongest and weakest performance areas in 5 seconds?

Can I identify which recent matches were exceptional without opening them?

Can I understand whether the selected match was better or worse than normal?

Can I identify the #1 coaching recommendation without reading everything?

Does this feel like PUBG Insight specifically, or could the logo be replaced and this become any SaaS analytics dashboard?

If the last answer is "any SaaS dashboard", the design is not finished.

==================================================
25. IMPLEMENTATION PROCESS
    ==================================================

Work in this order:

PHASE 1 — AUDIT
- inspect backend
- inspect frontend
- data coverage matrix
- UI problems
- typography inconsistencies
- semantic issues

PHASE 2 — DESIGN PLAN
Show me:

A. new page hierarchy
B. components to keep
C. components to redesign
D. components to create
E. available backend data newly surfaced
F. visualization chosen for each major metric
G. typography/capitalization rules

DO NOT MODIFY CODE YET.

STOP AND PRESENT THIS PLAN TO ME.

Wait for my approval.

Only after I approve:

PHASE 3 — IMPLEMENTATION
Implement section by section.

PHASE 4 — VISUAL QA
After implementation:
- inspect the rendered page
- check spacing
- check dead space
- check alignment
- check hierarchy
- check repeated rectangles
- check capitalization
- check AI item consistency
- check overflow
- check responsive layout

Do not declare the redesign complete purely because the code compiles.

==================================================
FINAL PRINCIPLE
==================================================

I do not want "a cleaner dashboard."

I want PUBG Insight to become a visually distinctive PLAYER ANALYSIS EXPERIENCE.

Use the data to create the design.

Do not decorate the existing layout.

Rethink it.