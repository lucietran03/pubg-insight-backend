I want a SECOND VISUAL-DESIGN PASS on the current PUBG Insight dashboard.

IMPORTANT:

Do NOT redesign the information architecture again.
Do NOT remove working functionality.
Do NOT change backend contracts.
Do NOT invent additional analytics.
Do NOT simply add more cards.

The current information architecture is substantially better.

This pass is specifically about:

1. visual composition
2. stronger hierarchy
3. performance encoding
4. better use of space
5. product personality
6. making analytics visually interesting rather than merely readable

The current dashboard is cleaner, but it still feels too much like:

dark rectangle
→ metric
→ dark rectangle
→ metric

I want it to feel like a PLAYER INTELLIGENCE EXPERIENCE rather than a generic SaaS dashboard.

==================================================
1. PLAYER DOSSIER — KEEP THE CARD, CHANGE COMPOSITION
   ==================================================

The current Player Overview content is good.

KEEP:

- avatar / initial
- player name
- platform
- recent match count
- Frontline Eliminator
- Combat 100/100 · Dominant trait
- archetype description

Do NOT remove any of these.

However, restructure the card horizontally.

LEFT SIDE:

Avatar
TGLTN
STEAM
140 matches · last 14 days

RIGHT SIDE:

Frontline Eliminator
Combat 100/100 · Dominant trait
Leads engagements and racks up kills at a high rate.

The right-side archetype information should feel like the player's
"combat identity".

Do not simply put another obvious rectangular card around it.

Use typography, a subtle divider/accent, emblem/badge treatment,
or another composition technique.

The whole module should feel like a PLAYER DOSSIER / PLAYER IDENTITY CARD.

Think:

IDENTITY                    PLAYSTYLE IDENTITY
TGLTN                        FRONTLINE ELIMINATOR
Steam                        Combat 100
140 matches                  Dominant Trait
Leads engagements...

Make the two sides visually balanced.

The player name remains the dominant identity element.

==================================================
2. CENTER SEASON PERFORMANCE
   ==================================================

The Season Performance hero currently looks visually stranded toward the left.

For:

10.0%

Season Win Rate · 1 win / 10 rounds (all modes)

CENTER the entire hero composition horizontally within the Season Performance section.

The win rate should remain visually dominant.

Hierarchy:

            10.0%

        SEASON WIN RATE

     1 win · 10 rounds
          all modes

or a similarly clean composition.

Do not center the SECTION TITLE itself.

"SEASON PERFORMANCE" remains left-aligned according to the global section-heading system.

Only the actual season-performance HERO content should be centered.

Use correct singular/plural grammar:

1 win
2 wins

1 round
2 rounds

Do not render "1 wins".

==================================================
3. PERFORMANCE BREAKDOWN — STRUCTURE IS GOOD,
   VISUAL EXECUTION IS NOT STRONG ENOUGH
   ==================================================

KEEP the conceptual hierarchy:

PRIMARY SIGNALS
SUPPORTING SIGNALS
PLAYER PROFILE

This structure works.

Current example:

PRIMARY SIGNALS

387
Avg Damage

3.67
K/D Ratio


SUPPORTING SIGNALS

24%
Headshot Rate

10%
Top 10 Rate

5m
Avg Survival

603m
Longest Kill

1.10×
Finish Rate


PLAYER PROFILE

Combat
Precision
Aggression
Consistency
Support
Survival

The INFORMATION is correct.

The PRESENTATION still feels generic.

==================================================
4. PRIMARY SIGNALS SHOULD FEEL PRIMARY
   ==================================================

387 Avg Damage
and
3.67 K/D

should immediately dominate the left-side analytics composition.

Do NOT make them simply two slightly larger versions of the supporting cards.

Explore a stronger metric composition.

For example:

387
AVG DAMAGE
━━━━━━━━━━

3.67
K/D RATIO

or another visually intentional layout.

They should feel like headline performance signals.

Use scale, typography, accent, dividers, or micro visualization.

Avoid unnecessary containers.

==================================================
5. SUPPORTING SIGNALS SHOULD NOT BE FIVE IDENTICAL CARDS
   ==================================================

This is one of the biggest remaining problems.

Currently:

24%
Headshot Rate

10%
Top 10 Rate

5m
Avg Survival

603m
Longest Kill

1.10×
Finish Rate

are still represented as repetitive rectangular stat cards.

Find a more sophisticated compact metric system.

Possible approaches:

- metric rail
- compact metric matrix
- horizontal indicator
- small performance bars where meaningful
- typographic metric clusters
- mini gauges
- annotated values
- micro visualizations
- different emphasis based on semantic meaning

Do NOT randomly choose five different styles.

They should belong to ONE visual system.

But that visual system should NOT simply be:

five dark boxes.

==================================================
6. PLAYER PROFILE / RADAR IS TOO SMALL
   ==================================================

The radar diagram is currently too visually weak.

Increase its size and visual presence.

It should become one of the main visual anchors of Performance Breakdown.

Do not make it gigantic, but it should compete visually with the primary metrics.

The user should immediately see the player's shape:

Combat 100
Precision 48
Aggression 77
Consistency 10
etc.

Improve:

- radar size
- label readability
- spacing around labels
- numerical emphasis
- contrast
- polygon visibility
- relationship between labels and chart

The radar should feel like the player's PERFORMANCE DNA.

Consider giving the radar area a subtle identity such as:

PLAYER PROFILE
Performance DNA

rather than presenting it like a generic chart.

Do NOT put another heavy card around the radar unless necessary.

==================================================
7. CONNECT METRICS TO THE RADAR
   ==================================================

Currently the left metrics and right radar feel like unrelated modules.

Create a stronger visual relationship.

Conceptually:

PRIMARY SIGNALS
387 DAMAGE
3.67 K/D

          → PLAYER PERFORMANCE DNA →

Combat 100
Aggression 77
Precision 48
Consistency 10
...

This does NOT require literal arrows.

Use composition, alignment, spacing, shared accent treatments,
or hierarchy to make them feel like one analytical story.

==================================================
8. RECENT MATCHES — ADD PERFORMANCE ENCODING
   ==================================================

Recent Matches is still too flat.

Example:

#1
10 kills
1379 damage

should visually stand out from:

#29
1 kill
185 damage

WITHOUT requiring the user to read every value.

Create restrained PERFORMANCE ENCODING.

Potential signals:

- excellent placement
- high damage
- high kill count
- poor performance
- unusually strong performance

Possible treatments:

- small performance accent
- placement intensity
- mini performance bar
- subtle glow/accent for exceptional performance
- compact "high impact" marker
- typography hierarchy

Do NOT use a rainbow.

Stay within the current dark + PUBG yellow system,
with restrained semantic positive/negative colors where appropriate.

IMPORTANT:

PERFORMANCE STATE
and
SELECTED STATE

must be visually different concepts.

A match can be excellent without being selected.

A selected match can be poor.

==================================================
9. MATCH ANALYSIS — KEEP THE NEW DEPTH
   ==================================================

The expanded Match Analysis is a strong improvement.

KEEP:

- Placement
- Map / Mode / Timestamp
- Combat & Survival
- Vs Season Average
- Weapons Used
- By Weapon
- By Body Part
- body silhouette / hit distribution

The body-part visualization is a GOOD example of what I want.

It transforms data into visual understanding.

Use this as the design benchmark for the rest of the dashboard:

DO NOT ASK:
"How can I put this number in a card?"

ASK:
"What relationship in this data should become visible?"

==================================================
10. AI INSIGHTS — STRUCTURE IS GOOD, VISUAL HIERARCHY NEEDS WORK
    ==================================================

KEEP the current information structure:

OVERALL VERDICT

WHAT YOU DID WELL
WHAT HURT YOUR PERFORMANCE

ONGOING RISK FACTORS

KEY COACHING ADVICE

PLAYSTYLE DIAGNOSIS

LONG-TERM DEVELOPMENT

Do not restructure this again.

However, the visual presentation still feels too much like
formatted AI output inside colored rectangles.

==================================================
11. KEY COACHING ADVICE MUST BECOME THE FOCAL POINT
    ==================================================

The most important outcome of AI Insights is:

"What should I do next?"

Therefore KEY COACHING ADVICE should visually dominate the report.

Current actions such as:

1. Slow down initial rotations
   FOCUS: Survival / Circle Play

2. Prioritize cover over aggressive peaks
   FOCUS: Aggression

3. Balance fragging with survival longevity
   FOCUS: Survival / Circle Play

should feel like a prioritized coaching program.

Make #1 clearly the primary recommendation.

For example:

01
SLOW DOWN INITIAL ROTATIONS

Survival / Circle Play
[brief explanation]

Then:

02
PRIORITIZE COVER OVER AGGRESSIVE PEAKS

03
BALANCE FRAGGING WITH SURVIVAL LONGEVITY

Do not put all three inside identical generic cards.

Use hierarchy.

The user should understand the #1 recommendation in approximately 2 seconds.

==================================================
12. AI CONTENT CONSISTENCY — FIX COMPLETELY
    ==================================================

Audit ALL generated insight rendering.

There have been inconsistent cases such as:

Excellent Flanking
excellent flanking

or the first generated item receiving different capitalization/style from later items.

This must NEVER depend on array position.

Search for:

:first-child
:first-of-type
index === 0
text-transform
capitalize
uppercase
conditional Typography variants
conditional fontWeight
conditional fontSize

All repeated insight items must use the same semantic component.

Define explicit rules:

SECTION LABEL
→ UPPERCASE

INSIGHT ITEM TITLE
→ consistent sentence case OR consistent Title Case

BODY
→ sentence case

FOCUS LABEL
→ UPPERCASE small label

Do NOT use CSS text-transform: capitalize on arbitrary AI-generated sentences.

Do NOT visually special-case item 0 unless it is intentionally a ranked coaching recommendation.

==================================================
13. CLEAN GENERATED AI TEXT
    ==================================================

I noticed output such as:

"lobby lobby wipe"

Do not allow obvious duplicated-word artifacts to appear in the polished UI.

Inspect where AI output is processed.

Add a conservative display sanitization step if appropriate,
without rewriting the meaning of generated content.

Also improve the AI prompt/schema if necessary so structured fields are preferred over uncontrolled formatting.

Do not aggressively modify generated prose.

==================================================
14. REDUCE THE RECTANGLE PROBLEM
    ==================================================

After implementing this pass, visually inspect the page.

Count how many obvious rectangular containers appear.

Ask whether each one is necessary.

Use:

spacing
alignment
typography
rules/dividers
visualization
background hierarchy
accent lines

to group information where possible.

Not every semantic group requires a border.

==================================================
15. ADD PRODUCT PERSONALITY
    ==================================================

The current interface successfully communicates:

PUBG tactical analytics ✓

But still weakly communicates:

player dossier
collectible profile
distinctive PUBG Insight identity
subtle cat/doll personality

Introduce a SMALL number of signature motifs.

Examples:

- archetype emblem
- collectible-profile detail
- distinctive metric marker
- subtle mascot/cat-inspired geometry
- small signature loading animation
- achievement/stamp treatment
- subtle corner/notch treatment

IMPORTANT:

Do NOT add random cat emojis.
Do NOT turn the interface pink.
Do NOT make it childish.
Do NOT sacrifice analytics readability.

The personality should be subtle enough that it feels like product identity,
not decoration.

==================================================
16. SIDEBAR
    ==================================================

The Recently Analyzed direction is better than vanity counters.

Keep it.

However, refine it so it feels like ANALYSIS HISTORY rather than tiny black cards.

Make scanning easier:

Map · Mode
Placement · Kills · Damage
short insight preview
time

Avoid excessive nested borders.

The sidebar should feel connected to the player-analysis workflow.

==================================================
17. VISUAL QUALITY TARGET
    ==================================================

After implementation, test these questions:

Can I instantly identify the player?

Can I instantly identify the season result?

Can I immediately identify Avg Damage and K/D as primary metrics?

Does the radar feel like a major performance visualization?

Can I identify the best recent match without reading all six cards?

Can I identify the #1 AI coaching recommendation in 2 seconds?

Does the page contain visual variety beyond rectangles?

Does this feel specifically like PUBG Insight?

If not, continue refining.

==================================================
18. IMPORTANT IMPLEMENTATION RULE
    ==================================================

DO NOT immediately edit everything.

First inspect the current implementation and tell me:

1. Which components you will modify
2. What visual change each component will receive
3. Which existing data each change uses
4. Which rectangles/cards can be removed
5. How the radar will be enlarged
6. How recent-match performance encoding will work
7. How AI coaching hierarchy will work
8. What subtle product-identity motif you propose

Then STOP.

Wait for my approval before implementing.

Do not redesign the information architecture again.

This is a VISUAL STORYTELLING PASS.