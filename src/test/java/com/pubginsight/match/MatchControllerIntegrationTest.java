package com.pubginsight.match;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgIncludedItem;
import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgMatchData;
import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgParticipantAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// See PlayerControllerIntegrationTest for the @MockitoBean version caveat.
@SpringBootTest
@AutoConfigureMockMvc
class MatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PubgApiClient pubgApiClient;

    @Test
    void getMatchStatsReturns200WithComputedHeadshotRate() throws Exception {
        PubgMatchData matchData = new PubgMatchData("match", "match-1",
                new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official"));
        PubgIncludedItem participant = new PubgIncludedItem("participant", "p-1",
                new PubgParticipantAttributes(new PubgParticipantStats(
                        "account.1", "shroud", 4, 2, 520.0, 1200.0, 1, 1, 0)));

        when(pubgApiClient.findMatchById("match-1"))
                .thenReturn(new PubgMatchResponse(matchData, List.of(participant)));

        mockMvc.perform(get("/api/players/account.1/matches/match-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headshotRate").value(0.5))
                .andExpect(jsonPath("$.winPlace").value(1))
                .andExpect(jsonPath("$.mapName").value("Erangel"));
    }

    @Test
    void getMatchStatsReturns404WhenMatchMissing() throws Exception {
        when(pubgApiClient.findMatchById("missing")).thenReturn(null);

        mockMvc.perform(get("/api/players/account.1/matches/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMatchStatsReturns404WhenPlayerDidNotParticipate() throws Exception {
        PubgMatchData matchData = new PubgMatchData("match", "match-1",
                new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official"));
        PubgIncludedItem otherParticipant = new PubgIncludedItem("participant", "p-2",
                new PubgParticipantAttributes(new PubgParticipantStats(
                        "account.other", "someoneElse", 1, 0, 100.0, 500.0, 10, 10, 0)));

        when(pubgApiClient.findMatchById("match-1"))
                .thenReturn(new PubgMatchResponse(matchData, List.of(otherParticipant)));

        mockMvc.perform(get("/api/players/account.1/matches/match-1"))
                .andExpect(status().isNotFound());
    }
}
