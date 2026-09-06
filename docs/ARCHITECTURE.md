# PUBG Insight — Architecture Documentation

This document is the technical source of truth for how the system is actually built, verified directly against the current source code (not assumptions or prior planning docs). It is written to double as the base material for the assignment's **Solution Architecture Document** and **Project Report → System Architecture** section (see `PROJECT_CONTEXT.md` → Deliverables).

Diagrams use [Mermaid](https://mermaid.js.org/) syntax, which GitHub renders natively in Markdown previews. They go from a high-level system view down to file-level call detail, per level:

1. System Context — the whole system and its external dependencies
2. Component View — packages/modules and their dependencies
3. Sequence Diagrams — request-by-request runtime behavior, one per feature
4. Data Mapping — how PUBG's raw wire format becomes this app's DTOs
5. Error Handling Flow — exception paths end to end

---

## 1. System Context

```mermaid
flowchart TB
    User(["User / Browser"])
    FE["React Frontend\n(pubg-insight-frontend)"]
    BE["Spring Boot Backend\n(pubg-insight-backend)"]
    PUBG[("PUBG Developer API\n(external)")]
    Gemini[("Google Gemini API\n(external, not yet integrated)")]
    AWS[("AWS\nElastic Beanstalk / API Gateway / Lambda\nDynamoDB / S3 / Athena\n(not yet integrated — see §7)")]

    User -->|HTTPS| FE
    FE -->|REST/JSON, axios| BE
    BE -->|REST, Bearer token| PUBG
    BE -.->|planned| Gemini
    BE -.->|planned| AWS

    style Gemini stroke-dasharray: 5 5
    style AWS stroke-dasharray: 5 5
```

**Hard rule enforced today, verified by code inspection**: the frontend has exactly one external call surface (`src/api/axios.ts`, `baseURL = VITE_API_BASE_URL`). It never imports or calls PUBG/Gemini/AWS directly — every such call happens through the backend.

---

## 2. Component View (Backend Package Structure)

The backend is organized **by feature, not by technical layer** (see `CLAUDE.md` → Backend Package Structure). Arrows show compile-time dependencies (which package imports which).

```mermaid
flowchart TB
    subgraph client["client/ — external API integrations"]
        pubg["client.pubg\nPubgApiClient, PubgApiProperties,\nPubgApiException, dto/*"]
        gemini["client.gemini\n(placeholder — Feature 3, not built)"]
    end

    subgraph common["common/ — cross-cutting concerns"]
        config["common.config\nCorsConfig"]
        exc["common.exception\nGlobalExceptionHandler"]
    end

    subgraph features["feature packages"]
        health["health\nHealthController"]
        player["player\nPlayerController, PlayerService,\nPlayerMapper, PlayerDto,\nSeasonStatsDto, PlayerNotFoundException"]
        match["match\nMatchController, MatchService,\nMatchMapper, MatchDto,\nMatchNotFoundException"]
    end

    player --> pubg
    match --> pubg
    exc --> player
    exc --> match
    exc --> pubg
```

Notable design point: `common.exception` depends on `player` and `match` (to catch their exceptions), but `player` and `match` never depend on each other, and neither depends on `common.exception`. This keeps features independent of one another — a change to `match/` cannot break `player/`.

### Frontend structure

```mermaid
flowchart TB
    App["App.tsx\n(root — health check + PlayerSearch)"]
    PS["components/PlayerSearch.tsx"]
    ML["components/MatchList.tsx"]
    SS["components/SeasonStats.tsx"]
    svcP["services/playerService.ts"]
    svcM["services/matchService.ts"]
    svcS["services/seasonStatsService.ts"]
    api["api/axios.ts\n(shared axios instance)"]
    types["types/*.ts\n(Player, Match, SeasonStats)"]
    err["utils/errorMessage.ts"]
    theme["theme.ts (PUBG-branded MUI theme)"]

    App --> PS
    PS --> ML
    PS --> SS
    PS --> svcP
    ML --> svcM
    SS --> svcS
    svcP --> api
    svcM --> api
    svcS --> api
    PS --> err
    ML --> err
    SS --> err
    svcP -.-> types
    svcM -.-> types
    svcS -.-> types
```

Each `service/*.ts` file mirrors exactly one backend controller's response shape via its matching `types/*.ts` interface — this pairing is intentional and should be kept in lockstep whenever a backend DTO changes.

---

## 3. Sequence Diagrams (per feature)

### 3.1 Player Search

```mermaid
sequenceDiagram
    actor U as User
    participant FE as PlayerSearch.tsx
    participant SVC as playerService.ts
    participant C as PlayerController
    participant S as PlayerService
    participant CL as PubgApiClient
    participant PUBG as PUBG API
    participant M as PlayerMapper

    U->>FE: type name, click Search
    FE->>SVC: searchPlayer(name)
    SVC->>C: GET /api/players/{name}
    C->>S: searchPlayerByName(name)
    S->>CL: findPlayerByName(name)
    CL->>PUBG: GET /shards/{shard}/players?filter[playerNames]={name}\n(Authorization: Bearer <key>)
    alt PUBG returns 404 (no match)
        PUBG-->>CL: 404
        CL-->>S: empty PubgPlayerListResponse
        S-->>C: throw PlayerNotFoundException
        C-->>SVC: 404 {error}
        SVC-->>FE: reject
        FE-->>U: "Could not find player"
    else PUBG returns data
        PUBG-->>CL: 200 PubgPlayerListResponse
        CL-->>S: PubgPlayerListResponse
        S->>M: toPlayerDto(data[0])
        M-->>S: PlayerDto
        S-->>C: PlayerDto
        C-->>SVC: 200 JSON
        SVC-->>FE: Player
        FE-->>U: renders name/shard/match count
    end
```

### 3.2 Match Analytics

Depends on Player Search having already run — `playerId` (PUBG account id) and a `matchId` (from `recentMatchIds`) are both already in the frontend's hands.

```mermaid
sequenceDiagram
    actor U as User
    participant FE as MatchList.tsx
    participant SVC as matchService.ts
    participant C as MatchController
    participant S as MatchService
    participant CL as PubgApiClient
    participant PUBG as PUBG API
    participant M as MatchMapper

    U->>FE: click a match chip
    FE->>SVC: getMatchStats(playerId, matchId)
    SVC->>C: GET /api/players/{playerId}/matches/{matchId}
    C->>S: getMatchStatsForPlayer(matchId, playerId)
    S->>CL: findMatchById(matchId)
    CL->>PUBG: GET /shards/{shard}/matches/{matchId}
    alt PUBG returns 404
        PUBG-->>CL: 404
        CL-->>S: null
        S-->>C: throw MatchNotFoundException
        C-->>FE: 404 {error}
    else PUBG returns match data
        PUBG-->>CL: 200 PubgMatchResponse (data + included[])
        CL-->>S: PubgMatchResponse
        S->>S: filter included[] where type=="participant"\nand stats.playerId == playerId
        alt no matching participant
            S-->>C: throw MatchNotFoundException
            C-->>FE: 404 {error}
        else participant found
            S->>M: toMatchDto(matchId, attributes, stats)
            M->>M: headshotRate = headshotKills / kills\n(0 if kills == 0)
            M-->>S: MatchDto
            S-->>C: MatchDto
            C-->>FE: 200 JSON
            FE-->>U: renders map, placement, kills,\nheadshot rate, damage, survival time
        end
    end
```

### 3.3 Season Stats (Win Rate)

Win rate cannot be derived from a single match — it is a season-level aggregate. This is a separate endpoint, fetched independently when a player is found (see Design Decision D5).

```mermaid
sequenceDiagram
    actor U as User
    participant FE as SeasonStats.tsx
    participant SVC as seasonStatsService.ts
    participant C as PlayerController
    participant S as PlayerService
    participant CL as PubgApiClient
    participant PUBG as PUBG API
    participant M as PlayerMapper

    Note over FE: mounts automatically when a player is found\n(key={player.id} forces remount per player)
    FE->>SVC: getSeasonStats(playerId)
    SVC->>C: GET /api/players/{playerId}/season-stats
    C->>S: getSeasonStats(accountId)
    S->>CL: findCurrentSeasonId()
    CL->>PUBG: GET /shards/{shard}/seasons
    PUBG-->>CL: PubgSeasonListResponse
    CL->>CL: find entry where isCurrentSeason == true
    CL-->>S: seasonId
    S->>CL: findSeasonStats(accountId, seasonId)
    CL->>PUBG: GET /shards/{shard}/players/{accountId}/seasons/{seasonId}
    alt PUBG returns 404
        PUBG-->>CL: 404
        CL-->>S: null
        S-->>C: throw PlayerNotFoundException
        C-->>FE: 404 {error}
    else PUBG returns season stats
        PUBG-->>CL: 200 PubgSeasonStatsResponse\n(gameModeStats: Map<mode, {wins, roundsPlayed}>)
        CL-->>S: PubgSeasonStatsResponse
        S->>M: toSeasonStatsDto(attributes)
        M->>M: sum wins & roundsPlayed across ALL modes\nwinRate = totalWins / totalRounds (0 if no rounds)
        M-->>S: SeasonStatsDto
        S-->>C: SeasonStatsDto
        C-->>FE: 200 JSON
        FE-->>U: renders "Season Win Rate: XX.X%"
    end
```

---

## 4. Data Mapping (PUBG raw JSON:API → internal DTOs)

PUBG's API follows the [JSON:API](https://jsonapi.org/) spec — deeply nested `data`/`attributes`/`relationships`/`included` objects. This app never exposes that shape to the frontend; every raw shape is mapped to a small, flat DTO by a dedicated `*Mapper` class.

```mermaid
classDiagram
    class PubgPlayerListResponse {
      +List~PubgPlayerData~ data
    }
    class PubgPlayerData {
      +String id
      +PubgPlayerAttributes attributes
      +PubgPlayerRelationships relationships
    }
    class PubgPlayerAttributes {
      +String name
      +String shardId
    }
    class PubgPlayerRelationships {
      +PubgRelationshipData matches
    }
    PubgPlayerListResponse --> PubgPlayerData
    PubgPlayerData --> PubgPlayerAttributes
    PubgPlayerData --> PubgPlayerRelationships

    class PlayerDto {
      +String id
      +String name
      +String shardId
      +List~String~ recentMatchIds
    }
    PubgPlayerData ..> PlayerDto : PlayerMapper.toPlayerDto()
```

```mermaid
classDiagram
    class PubgMatchResponse {
      +PubgMatchData data
      +List~PubgIncludedItem~ included
    }
    class PubgIncludedItem {
      +String type
      +PubgParticipantAttributes attributes
      note "only 'participant' type items\nhave meaningful attributes.stats;\nroster/asset entries are filtered out"
    }
    class PubgParticipantStats {
      +String playerId
      +Integer kills
      +Integer headshotKills
      +Double damageDealt
      +Double timeSurvived
      +Integer winPlace
    }
    PubgMatchResponse --> PubgIncludedItem
    PubgIncludedItem --> PubgParticipantAttributes
    PubgParticipantAttributes --> PubgParticipantStats

    class MatchDto {
      +String matchId
      +String mapName
      +int kills
      +int headshotKills
      +double headshotRate
      +double damageDealt
      +double timeSurvivedSeconds
      +int winPlace
    }
    PubgParticipantStats ..> MatchDto : MatchMapper.toMatchDto()\n(computes headshotRate, null-coalesces to 0)
```

```mermaid
classDiagram
    class PubgSeasonStatsResponse {
      +PubgSeasonStatsData data
    }
    class PubgSeasonStatsAttributes {
      +Map~String,PubgGameModeStats~ gameModeStats
    }
    class PubgGameModeStats {
      +Integer wins
      +Integer roundsPlayed
    }
    PubgSeasonStatsResponse --> PubgSeasonStatsAttributes
    PubgSeasonStatsAttributes --> PubgGameModeStats : one entry per\ngame mode (solo, duo, squad...)

    class SeasonStatsDto {
      +int wins
      +int roundsPlayed
      +double winRate
    }
    PubgGameModeStats ..> SeasonStatsDto : PlayerMapper.toSeasonStatsDto()\n(sums across ALL modes)
```

---

## 5. Error Handling Flow

```mermaid
flowchart TD
    A["PubgApiClient method call"] --> B{PUBG response}
    B -->|"404 (findPlayerByName)"| C["return empty PubgPlayerListResponse"]
    B -->|"404 (findMatchById / findSeasonStats)"| D["return null"]
    B -->|"other 4xx/5xx"| E["throw PubgApiException"]
    B -->|"network failure / timeout\n(ResourceAccessException)"| E
    B -->|200| F["return parsed response"]

    C --> G["Service layer checks emptiness"]
    D --> G
    F --> G
    G -->|empty/null| H["throw PlayerNotFoundException\nor MatchNotFoundException"]
    G -->|has data| I["map to app DTO, return 200"]

    H --> J["GlobalExceptionHandler.handleNotFound"]
    J --> K["404 {error: message}"]

    E --> L["GlobalExceptionHandler.handlePubgApiException"]
    L --> M["log.error(cause) — real failure logged server-side"]
    M --> N["502 {error: generic message}"]

    K --> O["Frontend: getErrorMessage(err, notFoundMsg)"]
    N --> O
    O -->|status==404| P["show notFoundMsg"]
    O -->|status is other 4xx/5xx| Q["show 'PUBG service unavailable'"]
    O -->|no response at all| R["show 'Could not reach server'"]
```

Two things this diagram makes visible that weren't true until today's fixes:
- The `ResourceAccessException` branch (network failure / timeout) didn't exist before — such failures used to escape uncaught into a generic Spring 500.
- The `log.error(cause)` step didn't exist before — `PubgApiException`'s real cause used to be silently discarded, which is exactly what made an earlier real debugging session (an empty API key producing an opaque 502) take much longer than it should have.

---

## 6. Design Decisions

Each entry: **Decision** — what was chosen. **Context** — why it came up. **Rationale** — why this option won. **Trade-off** — what was given up.

**D1 — Feature-based packages, not layer-based.**
Context: original scaffold used top-level `controller/`, `service/`, `dto/` etc. Rationale: everything a feature needs (controller, service, mapper, DTO, exceptions) lives together; adding a feature never means touching five different folders, and a feature can be understood by reading one directory. Trade-off: shared infrastructure (CORS, exception handling) needed its own `common/` package, and there's now a `PlayerNotFoundException`/`MatchNotFoundException` pair that look similar by design (see D2) rather than being unified.

**D2 — `PubgApiClient` never throws feature-specific exceptions.**
Context: discovered while building Match Analytics — the client originally threw `PlayerNotFoundException` on a 404, which is a `player`-package type the shared client shouldn't know about. Rationale: the client only understands PUBG's HTTP semantics (404, 5xx, network failure); each feature's *service* decides what "not found" means for it. Trade-off: every service that wraps a list/nullable client response has to repeat a small "if empty/null, throw" check — accepted as cheap and explicit rather than abstracting it prematurely.

**D3 — Spring's `RestClient`, synchronous, with explicit timeouts.**
Context: the app uses `spring-boot-starter-webmvc` (Servlet stack), not WebFlux — there's no reactive requirement anywhere in this app's scope. Rationale: `RestClient` is the modern synchronous HTTP client that fits the existing MVC stack without pulling in a reactive dependency for no reason. A 3s connect / 5s read timeout was added after the QA audit found none was set, meaning a hung PUBG response could hang a request thread indefinitely.

**D4 — 404 on list/nullable endpoints is normalized inside the client, not thrown.**
Context: PUBG returns HTTP 404 for "no player matches this name" and "no such match," which are normal, expected outcomes, not exceptional ones from the client's point of view. Rationale: `findPlayerByName` returns an empty list, `findMatchById`/`findSeasonStats` return `null`; the calling service decides whether that's a real error. Keeps the client's contract simple ("I always give you a parsed response or throw for a genuine failure").

**D5 — Win Rate is a separate endpoint (`/season-stats`), not bundled into Player Search or deferred to DynamoDB.**
Context: Win Rate is a required Match Analytics metric but can't be computed from a single match. Two alternatives were considered: (a) wait for Analysis History (DynamoDB) to aggregate stats from matches the user has viewed, (b) call PUBG's own season-stats endpoint. Rationale: (a) would only reflect the small subset of matches a user happened to click through this app — an inaccurate, incomplete win rate — and would block on AWS work not yet started. (b) gives PUBG's own authoritative season aggregate immediately, with no AWS dependency. Trade-off: costs two extra PUBG API calls (seasons list + season stats) per player, and PUBG doesn't return one lifetime figure — it returns a breakdown per game mode (see D6).

**D6 — Win rate is summed across all game modes into one number.**
Context: PUBG's season-stats response breaks wins/roundsPlayed down per mode (solo, duo, squad, and their FPP variants). Rationale: explicit product decision — a single combined number is simpler to read on a demo screen than six separate per-mode rates. Trade-off: a player who is excellent in squads but never plays solo will show a "blended" rate rather than their best mode's true rate.

**D7 — `application-local.yml` for secrets, not `.env`.**
Context: an early attempt used `.env` with `${PUBG_API_KEY:}` in `application-local.yml`, which silently resolved to an empty string because **Spring Boot does not read `.env` files** (that's a Vite/Node convention, not a Java one) — this caused a real, time-consuming 502 debugging session. Rationale: put the real value directly into the already-gitignored `application-local.yml`; Spring's profile mechanism (`spring.profiles.active: local`) loads it automatically regardless of how the app is launched (terminal vs IDE run button), avoiding the "env var set in one terminal, app launched from a different process" class of bug.

**D8 — AWS deployment target is the RMIT-provided Learner Lab, not a personal AWS account.**
Context: assignment requires real AWS usage but a personal account carries unpredictable real-money billing risk. Rationale: the Learner Lab is provided specifically for this coursework, with a fixed budget and no card-billing risk. Consequence: the not-yet-built AWS integration must use the Lab's pre-provisioned IAM role (no custom role/policy creation allowed) and expect compute to stop between sessions — mitigated for Elastic Beanstalk specifically because it hands out a stable URL that survives the underlying instance restarting.

**D9 — Frontend error messages are classified by HTTP response shape, not left generic.**
Context: QA audit found `PlayerSearch`/`MatchList`/`SeasonStats` all showed one generic message regardless of whether the real cause was "not found," "PUBG down," or "backend unreachable" — actively misleading for the second and third cases. Rationale: a small shared `getErrorMessage(err, notFoundMessage)` util branches on `err.response?.status` so a real backend outage no longer looks identical to "that player doesn't exist."

---

## 7. Not Yet Built (Planned Architecture)

The following are part of the approved architecture (`PROJECT_CONTEXT.md`) but have no code yet. Included here so this document stays the single reference as they land.

```mermaid
flowchart LR
    FE["React Frontend"] -->|HTTPS| EB["Elastic Beanstalk\n(Spring Boot backend)"]
    EB --> APIGW["API Gateway"]
    APIGW --> Lambda["Lambda\n(PUBG data retrieval/processing)"]
    Lambda --> PUBG[("PUBG API")]
    Lambda --> Gemini[("Gemini API")]
    EB --> DDB[("DynamoDB\nanalysis history")]
    EB --> S3[("S3\nmatch/report cache")]
    S3 --> Athena[("Athena\nanalytics queries")]
    Athena --> Dashboard["Analytics Dashboard\n(frontend)"]

    style FE fill:#333,color:#fff
    style EB fill:#f2a900,color:#000
```

All of the above must be triggered by application code — never a manual Console/CLI step — per the rubric's automation requirement (`CLAUDE.md` → Automation is graded, manual setup is not).

---

## 8. Known Limitations / Technical Debt

Carried over from the local-baseline QA audit; not blocking, but worth being aware of before building on top:

- `PubgApiClient` now serves three resource types (player, match, season) in one class — fine at its current size, worth splitting if a fourth (e.g. telemetry) is added.
- No caching of `findCurrentSeasonId()` — it's re-fetched on every season-stats call even though seasons change roughly every 2-3 months. Acceptable for now; would matter at real traffic volume.
- Backend unit tests exist for the mapper/service classes (see `src/test/java`) but could not be compiled/run in the environment they were written in — verify with `mvn test` before relying on them.
- `README.md` in both repos is stale (backend's still describes the old layer-based package plan and lists Spring Boot 3; frontend's is still the default Vite template) — this document supersedes them for architecture purposes, but the READMEs should eventually be updated to at least point here.
