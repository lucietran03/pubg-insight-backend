==================================================
IMPORTANT SCOPE BOUNDARY — NOT YET BUILT
==================================================

There are several V2 features that are part of the long-term product vision but are NOT yet implemented.

Do NOT build these features as part of this UI task unless explicitly instructed.

Do NOT fake missing backend data.

Do NOT create frontend-only workarounds that cause excessive PUBG API calls.

The current task is only to improve the existing comparison UI and AI Insights presentation using data that already exists.

The following features remain future work:

--------------------------------------------------
1. SEASON TREND CHARTS
--------------------------------------------------

Current state:

The current Season Performance implementation only compares:

Current Season
vs
Previous Season

using a single before/after delta per metric through the existing SeasonComparison logic.

This is NOT yet a real time-series implementation.

The V2 vision eventually wants trend charts for metrics such as:

- win rate
- average damage
- K/D
- headshot rate
- survival performance

However, PUBG does not provide a native "last N weeks" statistics endpoint.

The realistic future implementation would therefore use multiple historical seasons as the available time points.

This requires a backend change first.

Current backend behavior only retrieves ONE previous season.

Relevant backend logic includes:

- PubgApiClient.findPreviousSeasonId()
- PlayerService.getSeasonStats()

Future implementation should fetch several previous seasons and expose them through a dedicated historical-season response.

IMPORTANT:

Do NOT build a frontend trend chart using fabricated or duplicated points.

Do NOT simulate historical data.

Do NOT turn the current current-vs-previous delta into a fake line chart.

For this task, preserve the existing season comparison behavior.

--------------------------------------------------
2. DEEP INSIGHTS
--------------------------------------------------

The V2 product vision includes derived insights such as:

- favorite map
- highest-performing map
- lowest-performing map
- preferred game mode
- longest survival streak
- most aggressive match
- highest-damage match
- most efficient win
- teammate synergy
- other cross-match behavioral patterns

These are NOT currently available as aggregated backend analytics.

MatchDto already contains useful match-level data such as:

- mapName
- gameMode
- kills
- damage
- placement
- survival
- other per-match statistics

However, meaningful Deep Insights require aggregation across multiple matches.

Preferred future architecture:

Backend
→ aggregate cached/recent match data
→ derive cross-match insights
→ return one dedicated Deep Insights response

Do NOT implement this by making the frontend fetch every match individually.

Do NOT create an N-request frontend loop.

This is especially important because PUBG API access is rate-limited and repeated requests should be minimized.

Deep Insights require a new backend aggregation endpoint before they should be built in the frontend.

For this task:

Do NOT add fake Favorite Map, Best Map, Streak, Most Aggressive Match, etc. unless the backend already exposes real derived values.

--------------------------------------------------
3. BADGES / ACHIEVEMENTS / PROGRESS RINGS
--------------------------------------------------

This is the only future area that may be implemented frontend-only because much of the required data already exists.

Possible future presentation ideas:

- archetype badge
- high Top 10 rate badge
- high win-rate badge
- strong combat performance badge
- above-season-average match badge
- high survival performance badge

These may use already available:

- archetype
- radar scores
- match-vs-season deltas
- season statistics

However:

Do NOT claim achievements that cannot be proven by the available data.

For example:

"Personal Best Damage"

should only be displayed if historical match data confirms that it is actually the player's personal best.

If this cannot be verified, use evidence-based wording such as:

"High Damage Match"

or

"Above Season Average"

Badges are not part of the current required redesign unless they clearly improve the existing UI without introducing new logic or scope.

--------------------------------------------------
CURRENT TASK BOUNDARY
--------------------------------------------------

For the current redesign, focus ONLY on:

1. The "vs season average" comparison UI.

2. The existing AI Insights presentation:
    - AI Performance Summary
    - Strengths
    - Weaknesses
    - Recommendations
    - Playstyle Analysis
    - Season Progress
    - Risk Factors
    - Training Priorities

Use only data that the current frontend/backend already provides.

If you discover that a desired visual element requires unavailable backend data:

- do not fake it
- do not hardcode it
- do not derive it incorrectly in the frontend
- clearly mark it as a future backend dependency

Preserve product correctness over visual completeness.