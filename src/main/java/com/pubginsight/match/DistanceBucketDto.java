package com.pubginsight.match;

// One shot-distance range and how many of the player's kills in this match fell in it -
// e.g. ("0-30m", 4). Buckets mirror PUBG's own in-client "shot distance" breakdown ranges.
public record DistanceBucketDto(String label, int kills) {
}
