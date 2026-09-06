package com.pubginsight.player;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgPlayerAttributes;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
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

// Integration test: exercises the real Controller -> Service -> Mapper wiring through
// Spring's DI container and a real (simulated) HTTP request. Only the external PUBG
// boundary (PubgApiClient) is mocked, since we don't want real network calls in a test run.
//
// NOTE: written against Spring Boot 4.1.0's test annotations as best known; if
// @MockitoBean doesn't resolve, this Boot version may still use the older
// @org.springframework.boot.test.mock.mockito.MockBean instead — swap it if so.
@SpringBootTest
@AutoConfigureMockMvc
class PlayerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PubgApiClient pubgApiClient;

    @Test
    void searchExistingPlayerReturns200WithMappedFields() throws Exception {
        PubgPlayerData rawData = new PubgPlayerData("player", "account.999",
                new PubgPlayerAttributes("shroud", "steam", "pubg"), null);
        when(pubgApiClient.findPlayerByName("shroud"))
                .thenReturn(new PubgPlayerListResponse(List.of(rawData)));

        mockMvc.perform(get("/api/players/shroud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("account.999"))
                .andExpect(jsonPath("$.name").value("shroud"))
                .andExpect(jsonPath("$.shardId").value("steam"));
    }

    @Test
    void searchUnknownPlayerReturns404WithErrorBody() throws Exception {
        when(pubgApiClient.findPlayerByName("ghost"))
                .thenReturn(new PubgPlayerListResponse(List.of()));

        mockMvc.perform(get("/api/players/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
