package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgPlayerData(
        String type,
        String id,
        PubgPlayerAttributes attributes,
        PubgPlayerRelationships relationships
) {
}
