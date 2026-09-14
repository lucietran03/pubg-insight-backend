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
import com.pubginsight.client.s3.S3AnalyticsWriter;
import com.pubginsight.client.s3.S3MatchCacheClient;
import com.pubginsight.player.SeasonStatsCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Wires Controller -> InsightService -> PlayerService/MatchService for real through
// Spring; only the PUBG and Gemini clients are mocked.
@SpringBootTest
@AutoConfigureMockMvc
class InsightControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PubgApiClient pubgApiClient;

    @MockitoBean
    private GeminiApiClient geminiApiClient;

    // Without this, MatchService's real cache-aside read can hit AWS, letting a real S3
    // response - not the stubbed PubgApiClient - decide this test's behavior.
    @MockitoBean
    private S3MatchCacheClient s3MatchCacheClient;

    @MockitoBean
    private S3AnalyticsWriter s3AnalyticsWriter;

    // Same reason as s3MatchCacheClient above - without this, PlayerService's real
    // cache-aside read can hit AWS instead of the stubbed PubgApiClient deciding behavior.
    @MockitoBean
    private SeasonStatsCacheRepository seasonStatsCacheRepository;

    @BeforeEach
    void forceCacheMiss() {
        when(s3MatchCacheClient.getCachedMatchJson(anyString())).thenReturn(Optional.empty());
        when(seasonStatsCacheRepository.findByPlayerId(anyString())).thenReturn(Optional.empty());
    }

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
                        new PubgSeasonStatsAttributes(Map.of("squad", new PubgGameModeStats(
                                29, 265, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0, 0))))));

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
