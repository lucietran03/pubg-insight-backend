package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.insight.InsightDto;
import com.pubginsight.insight.InsightService;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// Re-derives match/insight data via MatchService/InsightService rather than storing it,
// which means recordAnalysis does one redundant PUBG match lookup (generateInsights fetches
// it again internally) - an accepted trade-off since this isn't a hot path.
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
