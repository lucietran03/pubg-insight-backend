package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgSeasonStatsData(String type, String id, PubgSeasonStatsAttributes attributes) {
}
