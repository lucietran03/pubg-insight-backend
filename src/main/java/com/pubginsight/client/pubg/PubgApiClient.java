package com.pubginsight.client.pubg;

import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class PubgApiClient {

    private static final String PUBG_JSON_API_MEDIA_TYPE = "application/vnd.api+json";

    private final RestClient restClient;
    private final String defaultShard;

    public PubgApiClient(PubgApiProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.key())
                .defaultHeader("Accept", PUBG_JSON_API_MEDIA_TYPE)
                .build();
        this.defaultShard = properties.defaultShard();
    }

    public PubgPlayerListResponse findPlayerByName(String playerName) {
        try {
            return restClient.get()
                    .uri("/shards/{shard}/players?filter[playerNames]={name}", defaultShard, playerName)
                    .retrieve()
                    .body(PubgPlayerListResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return new PubgPlayerListResponse(List.of());
        } catch (HttpStatusCodeException e) {
            throw new PubgApiException("PUBG API request failed for player '" + playerName + "'", e);
        }
    }
}
