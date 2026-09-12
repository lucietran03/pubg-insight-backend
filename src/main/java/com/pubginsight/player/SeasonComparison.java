package com.pubginsight.player;

// Percentage differences between the current season and the immediately preceding one,
// for the same metrics SeasonStatsDto already tracks. Positive means the current season
// is higher than the previous one. Computed by PlayerMapper.computeSeasonComparison() -
// null on SeasonStatsDto when there is no previous season to compare against (e.g. a
// brand-new game/shard).
public record SeasonComparison(
        double winRateDeltaPct,
        double avgDamageDeltaPct,
        double killDeathRatioDeltaPct,
        double headshotRateDeltaPct,
        double top10RateDeltaPct
) {
}
