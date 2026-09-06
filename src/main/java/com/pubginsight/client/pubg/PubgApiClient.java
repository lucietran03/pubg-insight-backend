package com.pubginsight.client.pubg;

import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonData;
import com.pubginsight.client.pubg.dto.PubgSeasonListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class PubgApiClient {

    private static final String PUBG_JSON_API_MEDIA_TYPE = "application/vnd.api+json";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;
    private final String defaultShard;

    public PubgApiClient(PubgApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
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
        } catch (HttpStatusCodeException | ResourceAccessException e) {
            throw new PubgApiException("PUBG API request failed for player '" + playerName + "'", e);
        }
    }

    public PubgMatchResponse findMatchById(String matchId) {
        try {
            return restClient.get()
                    .uri("/shards/{shard}/matches/{matchId}", defaultShard, matchId)
                    .retrieve()
                    .body(PubgMatchResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpStatusCodeException | ResourceAccessException e) {
            throw new PubgApiException("PUBG API request failed for match '" + matchId + "'", e);
        }
    }

    public String findCurrentSeasonId() {
        try {
            PubgSeasonListResponse response = restClient.get()
                    .uri("/shards/{shard}/seasons", defaultShard)
                    .retrieve()
                    .body(PubgSeasonListResponse.class);

            return response.data().stream()
                    .filter(season -> Boolean.TRUE.equals(season.attributes().isCurrentSeason()))
                    .map(PubgSeasonData::id)
                    .findFirst()
                    .orElseThrow(() -> new PubgApiException("No current PUBG season found", null));
        } catch (HttpStatusCodeException | ResourceAccessException e) {
            throw new PubgApiException("PUBG API request failed for seasons list", e);
        }
    }

    public PubgSeasonStatsResponse findSeasonStats(String accountId, String seasonId) {
        try {
            return restClient.get()
                    .uri("/shards/{shard}/players/{accountId}/seasons/{seasonId}", defaultShard, accountId, seasonId)
                    .retrieve()
                    .body(PubgSeasonStatsResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpStatusCodeException | ResourceAccessException e) {
            throw new PubgApiException("PUBG API request failed for season stats of '" + accountId + "'", e);
        }
    }
}
