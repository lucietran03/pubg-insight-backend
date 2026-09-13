package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.insight.InsightDto;
import com.pubginsight.insight.InsightService;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Wires Controller -> HistoryService -> AnalysisHistoryMapper for real through Spring;
// MatchService/InsightService and AnalysisHistoryRepository are mocked, so no real
// PUBG/Gemini/AWS call happens. Context startup also builds a real DynamoDbEnhancedClient
// bean, which is safe without AWS credentials since only an actual table operation would need them.
@SpringBootTest
@AutoConfigureMockMvc
class HistoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private InsightService insightService;

    @MockitoBean
    private AnalysisHistoryRepository analysisHistoryRepository;

    @Test
    void recordAnalysisReturns200WithSavedDto() throws Exception {
        when(matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .thenReturn(new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1, "2026-09-06T00:00:00Z"));
        when(insightService.generateInsights("account.1", "match-1"))
                .thenReturn(new InsightDto("Great match.", List.of("aim"), List.of("rotations"), List.of("play safer"),
                        "", "", List.of(), List.of(), "gemini"));

        mockMvc.perform(post("/api/players/account.1/matches/match-1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("account.1"))
                .andExpect(jsonPath("$.matchId").value("match-1"))
                .andExpect(jsonPath("$.insightSummary").value("Great match."))
                .andExpect(jsonPath("$.strengths[0]").value("aim"));

        verify(analysisHistoryRepository).save(any(AnalysisHistoryItem.class));
    }

    @Test
    void getHistoryReturns200WithMappedList() throws Exception {
        AnalysisHistoryItem item = new AnalysisHistoryItem("account.1", "match-1", "Erangel", "squad",
                4, 0.5, 520.0, 1200.0, 1, "Great match.",
                List.of("aim"), List.of("rotations"), List.of("play safer"), "2026-09-06T00:00:00Z");
        when(analysisHistoryRepository.findByPlayerId("account.1")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/players/account.1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value("account.1"))
                .andExpect(jsonPath("$[0].matchId").value("match-1"))
                .andExpect(jsonPath("$[0].insightSummary").value("Great match."));
    }
}
