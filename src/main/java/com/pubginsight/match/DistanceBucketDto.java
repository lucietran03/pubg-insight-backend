package com.pubginsight.match;

// Shot-distance bucket and kill count within it; ranges mirror PUBG's own in-client
// "shot distance" breakdown.
public record DistanceBucketDto(String label, int kills) {
}
