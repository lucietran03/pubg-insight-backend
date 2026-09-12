package com.pubginsight.player;

// Each axis is 0-100, scaled against a fixed reference ceiling (see PlayerMapper) rather
// than against other players - this app has no population of other users' stats to
// normalize against, so an absolute, documented ceiling is the honest choice over a
// fake percentile.
public record RadarScores(
        double combat,
        double survival,
        double precision,
        double aggression,
        double support,
        double consistency
) {
}
