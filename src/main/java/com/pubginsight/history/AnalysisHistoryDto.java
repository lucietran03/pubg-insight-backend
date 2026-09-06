package com.pubginsight.history;

import java.util.List;

public record AnalysisHistoryDto(
        String playerId,
        String matchId,
        String mapName,
        String gameMode,
        int kills,
        double headshotRate,
        double damageDealt,
        double timeSurvivedSeconds,
        int winPlace,
        String insightSummary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        String createdAt
) {
}
