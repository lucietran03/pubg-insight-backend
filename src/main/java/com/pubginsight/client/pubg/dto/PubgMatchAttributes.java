package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgMatchAttributes(String createdAt, Integer duration, String gameMode, String mapName, String matchType) {
}
