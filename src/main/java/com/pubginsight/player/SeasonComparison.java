package com.pubginsight.player;

// Percentage differences vs. the previous season for the same metrics SeasonStatsDto
// tracks. Positive means the current season is higher.
public record SeasonComparison(
        double winRateDeltaPct,
        double avgDamageDeltaPct,
        double killDeathRatioDeltaPct,
        double headshotRateDeltaPct,
        double top10RateDeltaPct,
        double avgSurvivalDeltaPct,
        double longestKillDeltaPct
) {
}
