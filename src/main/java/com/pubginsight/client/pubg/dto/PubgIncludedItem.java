package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// The "included" array mixes resource types with different attribute shapes; only
// "participant" is modeled, so other types deserialize with a null attributes.stats().
@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgIncludedItem(String type, String id, PubgParticipantAttributes attributes) {
}
