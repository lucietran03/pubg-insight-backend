package com.pubginsight.player;

public record SeasonStatsDto(
        int wins,
        int roundsPlayed,
        double winRate,
        double avgDamage,
        double killDeathRatio,
        double headshotRate,
        double top10Rate,
        double avgSurvivalSeconds,
        double longestKillMeters,
        RadarScores radar,
        String archetype,
        // Null when there is no previous season to compare against (e.g. a brand-new
        // game/shard) - see PlayerService.getSeasonStats and PlayerMapper.computeSeasonComparison.
        SeasonComparison previousSeasonComparison
) {

    // Comparison is attached after the fact since a single season has no visibility
    // into any other season.
    public SeasonStatsDto withPreviousSeasonComparison(SeasonComparison comparison) {
        return new SeasonStatsDto(wins, roundsPlayed, winRate, avgDamage, killDeathRatio, headshotRate,
                top10Rate, avgSurvivalSeconds, longestKillMeters, radar, archetype, comparison);
    }
}
