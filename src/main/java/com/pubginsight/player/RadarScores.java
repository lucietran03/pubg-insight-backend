package com.pubginsight.player;

// Each axis is 0-100, scaled against a fixed reference ceiling (see PlayerMapper)
// rather than against other players.
public record RadarScores(
        double combat,
        double survival,
        double precision,
        double aggression,
        double support,
        double consistency
) {
}
