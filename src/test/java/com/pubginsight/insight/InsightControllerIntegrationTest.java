package com.pubginsight.insight;

import com.pubginsight.client.gemini.GeminiApiClient;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgGameModeStats;
import com.pubginsight.client.pubg.dto.PubgIncludedItem;
import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgMatchData;
import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgParticipantAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsAttributes;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsData;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises Controller -> InsightService -> PlayerService/MatchService, wired for real
// through Spring. Only the two external client boundaries (PUBG, Gemini) are mocked.
// See PlayerControllerIntegrationTest for the @MockitoBean version caveat.
@SpringBootTest
@AutoConfigureMockMvc
class InsightControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PubgApiClient pubgApiClient;

    @MockitoBean
    private GeminiApiClient geminiApiClient;

    @Test
    void getInsightsReturns200WithParsedSections() throws Exception {
        PubgMatchData matchData = new PubgMatchData("match", "match-1",
                new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official"));
        PubgIncludedItem participant = new PubgIncludedItem("participant", "p-1",
                new PubgParticipantAttributes(new PubgParticipantStats(
                        "account.1", "shroud", 4, 2, 520.0, 1200.0, 1, 1, 0)));
        when(pubgApiClient.findMatchById("match-1"))
                .thenReturn(new PubgMatchResponse(matchData, List.of(participant)));

        when(pubgApiClient.findCurrentSeasonId()).thenReturn("season-current");
        when(pubgApiClient.findSeasonStats("account.1", "season-current"))
                .thenReturn(new PubgSeasonStatsResponse(new PubgSeasonStatsData("playerSeason", "id",
                        new PubgSeasonStatsAttributes(Map.of("squad", new PubgGameModeStats(29, 265))))));

        when(geminiApiClient.generateText(anyString())).thenReturn(
                "SUMMARY: Great aim this match.\n"
                        + "STRENGTHS: headshots, damage\n"
                        + "WEAKNESSES: rotations\n"
                        + "RECOMMENDATIONS: play safer zones\n");

        mockMvc.perform(get("/api/players/account.1/matches/match-1/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Great aim this match."))
                .andExpect(jsonPath("$.strengths[0]").value("headshots"))
                .andExpect(jsonPath("$.recommendations[0]").value("play safer zones"));
    }
}
