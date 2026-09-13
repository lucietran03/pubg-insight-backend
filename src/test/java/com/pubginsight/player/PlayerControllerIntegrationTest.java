package com.pubginsight.player;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.PubgRateLimitException;
import com.pubginsight.client.pubg.dto.PubgPlayerAttributes;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Written against Spring Boot 4.1.0's @MockitoBean; if it doesn't resolve, this Boot
// version may need the older @MockBean instead.
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

    @Test
    void searchPlayerReturns429WithRetryAfterWhenPubgRateLimits() throws Exception {
        when(pubgApiClient.findPlayerByName("busy")).thenThrow(new PubgRateLimitException(30L));

        mockMvc.perform(get("/api/players/busy"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.error").exists());
    }
}
