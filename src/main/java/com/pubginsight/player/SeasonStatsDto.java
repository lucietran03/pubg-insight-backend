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

    // toSeasonStatsDto() builds a SeasonStatsDto for a single season without knowing
    // about any other season - PlayerService composes the comparison afterwards (it's the
    // one that fetches both seasons), then attaches it here rather than threading a second
    // dto through the mapper's core aggregation method.
    public SeasonStatsDto withPreviousSeasonComparison(SeasonComparison comparison) {
        return new SeasonStatsDto(wins, roundsPlayed, winRate, avgDamage, killDeathRatio, headshotRate,
                top10Rate, avgSurvivalSeconds, longestKillMeters, radar, archetype, comparison);
    }
}
