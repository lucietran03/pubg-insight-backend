package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgSeasonStatsAttributes(Map<String, PubgGameModeStats> gameModeStats) {
}
