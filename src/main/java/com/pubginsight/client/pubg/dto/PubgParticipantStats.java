package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgParticipantStats(
        String playerId,
        String name,
        Integer kills,
        Integer headshotKills,
        Double damageDealt,
        Double timeSurvived,
        Integer winPlace,
        Integer killPlace,
        Integer assists
) {
}
