package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgSeasonAttributes(Boolean isCurrentSeason, Boolean isOffseason) {
}
