I am building a project called **PUBG Insight – an AI-powered PUBG Player Performance Analytics Platform**.

I want you to research and help me design the best technical approach for this idea. Do **not** assume any existing implementation. Treat this as an architecture and implementation research task from scratch.

## Project Idea

The platform allows a user to search for a PUBG player using their **PUBG username/player name**.

Example:

`Player Name → PUBG Account ID → Player Data → Match Data → Telemetry → Analytics → Skill Profile → AI Insights`

The goal is NOT simply to reproduce raw PUBG statistics.

I want to transform PUBG data into meaningful player-performance analytics that can answer questions such as:

- How skilled is this player?
- What type of player are they?
- What are their strengths and weaknesses?
- How aggressive/passive are they?
- How good are they at combat?
- How well do they survive?
- How consistent are they?
- Are they improving or declining?
- How does their recent performance compare with their historical/season performance?

Potential output could include:

- Overall Skill Score
- Combat Score
- Survival Score
- Precision / Accuracy Score
- Aggression Score
- Consistency Score
- Recent Performance Trend
- Strengths
- Weaknesses
- Player archetype/playstyle
- AI-generated performance explanation

For example:

`Player: TGLTN`

`Overall Skill: 87/100`

`Combat: 92`
`Survival: 78`
`Consistency: 84`
`Aggression: 91`
`Precision: 86`

`Playstyle: Aggressive Fragger`

The system should calculate the quantitative analytics itself. An LLM/AI should mainly be used to **interpret the calculated data and generate human-readable insights**, rather than sending huge amounts of raw telemetry directly to an LLM.

---

## Main Technical Problem

The official PUBG Developer API has relatively restrictive rate limits for normal developer API keys.

Therefore, I do NOT want an architecture that repeatedly calls PUBG endpoints whenever the frontend loads or whenever a player is searched.

I want you to investigate how to build this efficiently using:

- PUBG player lookup
- account IDs
- season/lifetime/ranked statistics
- recent matches
- match data
- telemetry
- weapon/mastery data if useful
- caching
- persistent storage
- asynchronous/background processing if appropriate

Pay particular attention to which PUBG endpoints are rate-limited and which requests, such as match/telemetry retrieval, may be treated differently.

I want to minimize unnecessary API calls while still providing detailed analytics.

Think about a flow similar to:

`Player Search`
→ `Resolve accountId`
→ `Check cached player`
→ `Retrieve/update statistics when necessary`
→ `Retrieve recent match IDs`
→ `Retrieve match data`
→ `Retrieve telemetry`
→ `Store/cache reusable raw data`
→ `Analytics Engine`
→ `Performance Profile`
→ `AI Insight`
→ `Cache/store final analysis`

The same player or match should NOT need to be fetched and processed repeatedly if the data already exists.

---

## GitHub Repositories / Resources to Study

Please inspect these repositories/resources as references. Do not blindly copy them; determine what concepts, models, API patterns, utilities, or architecture ideas are still useful.

### PUBG Java

https://github.com/mautini/pubgjava

Study how it handles:

- Player lookup
- Player IDs
- Matches
- Season statistics
- Telemetry
- PUBG API models
- API client structure

Determine which parts are still compatible with the current PUBG API and which are outdated.

### SoftwareSandbox PUBG API Client

https://github.com/SoftwareSandbox/pubg-api-client

Study its approach to:

- Player lookup by name
- Player lookup by ID
- Match relationships
- PUBG API requests
- Java models
- API client abstraction

Again, check whether its implementation/API assumptions remain current.

### PUBG-Kt

https://github.com/theodorosidmar/pubgkt

Although this is Kotlin, study its API/domain organization, especially around:

- Players
- Stats
- Matches
- Mastery
- Leaderboards
- PUBG domain models

Look for architectural ideas that could translate well to a Java backend.

### Official PUBG API Assets

https://github.com/pubg/api-assets

Study the official PUBG dictionaries/assets for interpreting telemetry and API values instead of manually hardcoding mappings where possible.

Also use the current official PUBG Developer API documentation as the authoritative source whenever GitHub repositories conflict with the current API.

---

## Technical Direction

The intended backend technology is:

- **Java**
- **Spring Boot**

The larger system is intended to be cloud-oriented, so AWS services may be used where they genuinely improve the architecture.

Possible services/technologies can include:

- DynamoDB
- S3
- AWS Lambda
- SQS
- EventBridge
- ElastiCache/Redis
- API Gateway
- CloudWatch

However, do NOT add AWS services merely to make the architecture look complex.

Every service should solve a clear problem such as:

- caching
- persistent analytics storage
- asynchronous telemetry processing
- scheduled refresh
- rate-limit protection
- scalability
- observability

---

## Analytics Engine

A major part of your research should focus on **what can realistically be derived from PUBG API + telemetry data**.

Identify the exact fields/events that could support metrics such as:

### Combat

Possible factors:

- kills
- assists
- damage
- DBNOs
- headshots
- kill distance
- damage per match
- kills per match
- engagement frequency

### Survival

Possible factors:

- placement
- survival time
- late-game frequency
- distance travelled
- win/top-10 frequency

### Aggression

Potential signals:

- fights/engagements per unit time
- early-game combat
- damage frequency
- kills
- movement toward engagements
- combat participation

Do not claim a metric can be calculated unless the necessary data actually exists.

### Consistency

Potential approach:

Analyze variance/standard deviation across recent matches for metrics such as:

- damage
- kills
- placement
- survival
- combined performance score

### Precision

Investigate whether true weapon accuracy can actually be calculated from available telemetry.

If exact shots-fired / shots-hit information is unavailable or unreliable, propose a more defensible alternative metric rather than calling something "accuracy" incorrectly.

### Performance Trend

Compare recent matches against:

- previous matches
- rolling averages
- season baseline

Determine a statistically reasonable method.

### Player Archetype

Explore whether derived metrics could classify players into understandable playstyles such as:

- Aggressive Fragger
- Survival-focused
- Balanced
- Precision-focused
- High-risk / high-reward
- Consistent all-rounder

The classification should ideally be deterministic from analytics first, with AI explaining the result rather than inventing the classification.

---

## Important Research Question: Skill Scoring

Do NOT arbitrarily create formulas such as:

`Skill = kills × 0.4 + damage × 0.3 + placement × 0.3`

without justification.

Research how PUBG performance can reasonably be normalized.

Consider approaches such as:

- percentile scoring
- z-score normalization
- min-max normalization
- population baselines
- season baselines
- rolling player baselines
- game-mode-specific normalization
- weighted composite indices

Consider that PUBG statistics vary significantly depending on:

- Solo / Duo / Squad
- FPP / TPP
- ranked vs normal
- number of matches
- season
- player skill population

Recommend a method that is explainable and realistic for a university cloud analytics project.

---

## AI Layer

The AI should receive a compact structured profile rather than raw match telemetry.

For example:

```json
{
  "player": "TGLTN",
  "matchesAnalyzed": 20,
  "combatScore": 92,
  "survivalScore": 78,
  "consistencyScore": 84,
  "aggressionScore": 91,
  "recentTrend": 0.13,
  "strengths": [
    "high damage output",
    "strong close-range engagements"
  ],
  "weaknesses": [
    "below-baseline late-game survival"
  ],
  "playstyle": "Aggressive Fragger"
}
```

Then the AI can explain the profile in natural language.

This separation is important:

`PUBG Data → Deterministic Analytics → Structured Profile → LLM Explanation`

rather than:

`Huge PUBG JSON → LLM → hope it understands everything`

Research how to make this AI layer:

- inexpensive
- cacheable
- reproducible
- resistant to hallucination
- useful to players

---

## Rate-Limit & Caching Strategy

Design a concrete caching policy.

For example, determine appropriate TTL/update behavior for:

- `playerName → accountId`
- player profile
- current season stats
- ranked stats
- match IDs
- match objects
- telemetry
- calculated analytics
- AI insights

Think carefully about immutable data.

A completed PUBG match should generally not need to be downloaded repeatedly if its match/telemetry data has already been stored.

Likewise, AI analysis should not need to be regenerated when the underlying analytics input has not changed.

Also investigate:

- request deduplication
- concurrent requests for the same player
- rate-limit-aware queues
- retry/backoff
- PUBG API response headers
- batching opportunities
- parallel retrieval where safe
- asynchronous telemetry processing

---

## What I Want From You

After researching the official PUBG API and the referenced repositories, propose an implementation-ready architecture.

I specifically want:

1. The best end-to-end player-search and analysis flow.
2. The exact PUBG endpoints needed.
3. Which endpoints count toward rate limits.
4. Which data should be cached or permanently stored.
5. Recommended cache TTLs.
6. Which telemetry events/fields are useful.
7. Which statistics can realistically be calculated.
8. A defensible Skill Score methodology.
9. Combat / Survival / Aggression / Consistency / Precision scoring approaches.
10. Player archetype classification logic.
11. Recent-performance trend methodology.
12. How to avoid misleading analytics.
13. How the Java/Spring Boot services should be separated.
14. Suggested DTO/domain models.
15. Suggested API endpoints for my own frontend.
16. AWS services that genuinely improve this architecture.
17. How to minimize PUBG API usage.
18. How to minimize LLM/API usage.
19. Ideas from the referenced GitHub repositories worth adopting.
20. Ideas/code patterns from those repositories that should NOT be adopted because they are outdated or poorly suited to this project.

Prioritize **practical implementation**, not theoretical architecture.

Where possible, support recommendations with current PUBG API documentation and actual fields/events available from PUBG telemetry.

The final solution should be realistic for a university cloud-computing project while still being technically impressive enough to demonstrate:

- external API integration
- cloud architecture
- data processing
- analytics
- caching/storage
- scalability
- AI integration

Most importantly, challenge my assumptions where necessary. If a proposed metric cannot actually be supported by PUBG's available data, say so and propose a better metric instead.