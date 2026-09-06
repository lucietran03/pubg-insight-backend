package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.insight.InsightDto;
import com.pubginsight.match.MatchDto;
import org.springframework.stereotype.Component;

// Pure mapping between the DynamoDB-facing AnalysisHistoryItem and the app-facing
// AnalysisHistoryDto/(MatchDto + InsightDto) pair. createdAt is passed in rather than
// generated here (e.g. via Instant.now()) so this class stays a plain, deterministic
// function that's trivial to unit test without mocking the clock.
@Component
public class AnalysisHistoryMapper {

    public AnalysisHistoryItem toItem(String playerId, String matchId, MatchDto match, InsightDto insight, String createdAt) {
        return new AnalysisHistoryItem(
                playerId,
                matchId,
                match.mapName(),
                match.gameMode(),
                match.kills(),
                match.headshotRate(),
                match.damageDealt(),
                match.timeSurvivedSeconds(),
                match.winPlace(),
                insight.summary(),
                insight.strengths(),
                insight.weaknesses(),
                insight.recommendations(),
                createdAt
        );
    }

    public AnalysisHistoryDto toDto(AnalysisHistoryItem item) {
        return new AnalysisHistoryDto(
                item.getPlayerId(),
                item.getMatchId(),
                item.getMapName(),
                item.getGameMode(),
                item.getKills() == null ? 0 : item.getKills(),
                item.getHeadshotRate() == null ? 0.0 : item.getHeadshotRate(),
                item.getDamageDealt() == null ? 0.0 : item.getDamageDealt(),
                item.getTimeSurvivedSeconds() == null ? 0.0 : item.getTimeSurvivedSeconds(),
                item.getWinPlace() == null ? 0 : item.getWinPlace(),
                item.getInsightSummary(),
                item.getStrengths(),
                item.getWeaknesses(),
                item.getRecommendations(),
                item.getCreatedAt()
        );
    }
}
