package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.insight.InsightDto;
import com.pubginsight.insight.InsightService;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// Orchestrates Match + Insight features to gather what's already been computed, stamps a
// creation timestamp, then persists the result - the same "compose existing services"
// pattern InsightService itself uses for player/match (see docs/deliverables/ARCHITECTURE.md D10).
//
// Note: generateInsights() internally re-fetches match stats via MatchService itself, so
// calling both matchService.getMatchStatsForPlayer(...) and
// insightService.generateInsights(...) here does one redundant PUBG match lookup. This is
// an accepted trade-off for reusing InsightService's existing composition/parsing logic
// as-is rather than splitting it apart - recordAnalysis is an on-demand, user-triggered
// action, not a hot path.
@Service
public class HistoryService {

    private final MatchService matchService;
    private final InsightService insightService;
    private final AnalysisHistoryRepository analysisHistoryRepository;
    private final AnalysisHistoryMapper analysisHistoryMapper;

    public HistoryService(MatchService matchService, InsightService insightService,
            AnalysisHistoryRepository analysisHistoryRepository, AnalysisHistoryMapper analysisHistoryMapper) {
        this.matchService = matchService;
        this.insightService = insightService;
        this.analysisHistoryRepository = analysisHistoryRepository;
        this.analysisHistoryMapper = analysisHistoryMapper;
    }

    public AnalysisHistoryDto recordAnalysis(String playerId, String matchId) {
        MatchDto match = matchService.getMatchStatsForPlayer(matchId, playerId);
        InsightDto insight = insightService.generateInsights(playerId, matchId);

        String createdAt = Instant.now().toString();
        AnalysisHistoryItem item = analysisHistoryMapper.toItem(playerId, matchId, match, insight, createdAt);

        analysisHistoryRepository.save(item);
        return analysisHistoryMapper.toDto(item);
    }

    public List<AnalysisHistoryDto> getHistoryForPlayer(String playerId) {
        return analysisHistoryRepository.findByPlayerId(playerId).stream()
                .map(analysisHistoryMapper::toDto)
                .toList();
    }
}
