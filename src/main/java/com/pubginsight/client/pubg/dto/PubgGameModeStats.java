package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// PUBG's real per-mode season stats payload has dozens of fields; this only lists the
// ones this app actually derives metrics from (see PlayerMapper/PlayerAnalyticsService).
// @JsonIgnoreProperties means adding more fields later is always safe/additive - nothing
// here needs to change if PUBG adds new fields we still don't care about.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgGameModeStats(
        Integer wins,
        Integer roundsPlayed,
        Integer losses,
        Integer kills,
        Integer assists,
        Integer headshotKills,
        Double damageDealt,
        Double timeSurvived,
        Double longestTimeSurvived,
        Double longestKill,
        Integer top10s,
        Integer revives,
        Integer roadKills,
        Integer teamKills,
        Integer heals,
        Integer boosts,
        Double rideDistance,
        Double walkDistance,
        Integer weaponsAcquired,
        Integer dBNOs
) {
}
