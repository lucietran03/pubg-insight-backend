package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// PUBG's per-mode season stats payload has dozens of fields; only the ones this app
// actually uses are modeled here.
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
