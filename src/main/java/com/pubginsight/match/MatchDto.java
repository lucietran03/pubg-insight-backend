package com.pubginsight.match;

public record MatchDto(
        String matchId,
        String mapName,
        String gameMode,
        int kills,
        int headshotKills,
        double headshotRate,
        double damageDealt,
        double timeSurvivedSeconds,
        int winPlace
) {
}
