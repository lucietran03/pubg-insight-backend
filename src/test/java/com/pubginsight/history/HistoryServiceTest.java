package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.insight.InsightDto;
import com.pubginsight.insight.InsightService;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private MatchService matchService;

    @Mock
    private InsightService insightService;

    @Mock
    private AnalysisHistoryRepository analysisHistoryRepository;

    @Mock
    private AnalysisHistoryMapper analysisHistoryMapper;

    @InjectMocks
    private HistoryService historyService;

    @Test
    void recordAnalysisComposesMatchAndInsightThenSaves() {
        MatchDto match = new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1, "2026-09-06T00:00:00Z");
        InsightDto insight = new InsightDto("Great match.", List.of("aim"), List.of("rotations"), List.of("play safer"),
                "", "", List.of(), List.of(), "gemini");
        AnalysisHistoryItem item = new AnalysisHistoryItem("account.1", "match-1", "Erangel", "squad",
                4, 0.5, 520.0, 1200.0, 1, "Great match.",
                List.of("aim"), List.of("rotations"), List.of("play safer"), "2026-09-06T00:00:00Z");
        AnalysisHistoryDto expectedDto = new AnalysisHistoryDto("account.1", "match-1", "Erangel", "squad",
                4, 0.5, 520.0, 1200.0, 1, "Great match.",
                List.of("aim"), List.of("rotations"), List.of("play safer"), "2026-09-06T00:00:00Z");

        when(matchService.getMatchStatsForPlayer("match-1", "account.1")).thenReturn(match);
        when(insightService.generateInsights("account.1", "match-1")).thenReturn(insight);
        when(analysisHistoryMapper.toItem(eq("account.1"), eq("match-1"), eq(match), eq(insight), anyString()))
                .thenReturn(item);
        when(analysisHistoryMapper.toDto(item)).thenReturn(expectedDto);

        AnalysisHistoryDto result = historyService.recordAnalysis("account.1", "match-1");

        verify(analysisHistoryRepository).save(item);
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void getHistoryForPlayerMapsAllItemsToDtos() {
        AnalysisHistoryItem item = new AnalysisHistoryItem("account.1", "match-1", "Erangel", "squad",
                4, 0.5, 520.0, 1200.0, 1, "Great match.",
                List.of("aim"), List.of("rotations"), List.of("play safer"), "2026-09-06T00:00:00Z");
        AnalysisHistoryDto dto = new AnalysisHistoryDto("account.1", "match-1", "Erangel", "squad",
                4, 0.5, 520.0, 1200.0, 1, "Great match.",
                List.of("aim"), List.of("rotations"), List.of("play safer"), "2026-09-06T00:00:00Z");

        when(analysisHistoryRepository.findByPlayerId("account.1")).thenReturn(List.of(item));
        when(analysisHistoryMapper.toDto(item)).thenReturn(dto);

        List<AnalysisHistoryDto> result = historyService.getHistoryForPlayer("account.1");

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getHistoryForPlayerReturnsEmptyListWhenNoHistoryStored() {
        when(analysisHistoryRepository.findByPlayerId("account.2")).thenReturn(List.of());

        List<AnalysisHistoryDto> result = historyService.getHistoryForPlayer("account.2");

        assertThat(result).isEmpty();
    }
}
