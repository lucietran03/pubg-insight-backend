package com.pubginsight.client.pubg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// PUBG's "included" array mixes roster/participant/asset resources with different attribute
// shapes; only "participant" is modeled here. Other types just deserialize with a null
// PubgParticipantAttributes.stats() and get filtered out by type before that's ever read.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PubgIncludedItem(String type, String id, PubgParticipantAttributes attributes) {
}
