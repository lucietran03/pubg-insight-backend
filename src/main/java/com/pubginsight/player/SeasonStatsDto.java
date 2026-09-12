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
        String archetype
) {
}
