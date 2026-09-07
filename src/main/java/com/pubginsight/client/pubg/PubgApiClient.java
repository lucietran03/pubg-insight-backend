package com.pubginsight.client.pubg;

import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonData;
import com.pubginsight.client.pubg.dto.PubgSeasonListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class PubgApiClient {

    private static final String PUBG_JSON_API_MEDIA_TYPE = "application/vnd.api+json";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;
    private final String defaultShard;
    private final PubgRateLimiter rateLimiter;

    // The current season changes roughly every 2-3 months, so caching it for the life of
    // the app instance saves 1 PUBG call per season-stats request - meaningful given the
    // 10 req/min free-tier limit. A restart is enough to pick up a season change.
    private volatile String cachedSeasonId;

    public PubgApiClient(PubgApiProperties properties, PubgRateLimiter rateLimiter) {
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
        this.rateLimiter = rateLimiter;
    }

    public PubgPlayerListResponse findPlayerByName(String playerName) {
        rateLimiter.acquire();
        try {
            return restClient.get()
                    .uri("/shards/{shard}/players?filter[playerNames]={name}", defaultShard, playerName)
                    .retrieve()
                    .body(PubgPlayerListResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return new PubgPlayerListResponse(List.of());
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            // Catching RestClientException itself, not just its ResourceAccessException/
            // HttpStatusCodeException subtypes: a read timeout that happens while Spring is
            // still reading response headers/body (readWithMessageConverters) surfaces as a
            // plain RestClientException, not ResourceAccessException - confirmed via a real
            // uncaught 500 from GeminiApiClient's identical old catch clause in production
            // logs. PubgApiClient had the same gap, just not yet triggered by its shorter
            // 5s read timeout.
            throw new PubgApiException("PUBG API request failed for player '" + playerName + "'", e);
        }
    }

    public PubgMatchResponse findMatchById(String matchId) {
        rateLimiter.acquire();
        try {
            return restClient.get()
                    .uri("/shards/{shard}/matches/{matchId}", defaultShard, matchId)
                    .retrieve()
                    .body(PubgMatchResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            throw new PubgApiException("PUBG API request failed for match '" + matchId + "'", e);
        }
    }

    public String findCurrentSeasonId() {
        String cached = cachedSeasonId;
        if (cached != null) {
            return cached;
        }

        rateLimiter.acquire();
        try {
            PubgSeasonListResponse response = restClient.get()
                    .uri("/shards/{shard}/seasons", defaultShard)
                    .retrieve()
                    .body(PubgSeasonListResponse.class);

            String seasonId = response.data().stream()
                    .filter(season -> Boolean.TRUE.equals(season.attributes().isCurrentSeason()))
                    .map(PubgSeasonData::id)
                    .findFirst()
                    .orElseThrow(() -> new PubgApiException("No current PUBG season found", null));

            cachedSeasonId = seasonId;
            return seasonId;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            throw new PubgApiException("PUBG API request failed for seasons list", e);
        }
    }

    public PubgSeasonStatsResponse findSeasonStats(String accountId, String seasonId) {
        rateLimiter.acquire();
        try {
            return restClient.get()
                    .uri("/shards/{shard}/players/{accountId}/seasons/{seasonId}", defaultShard, accountId, seasonId)
                    .retrieve()
                    .body(PubgSeasonStatsResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            throw new PubgApiException("PUBG API request failed for season stats of '" + accountId + "'", e);
        }
    }

    // PUBG's Retry-After (when present) is the delay-seconds form, not an HTTP-date -
    // parse defensively and simply omit it if it's not a plain integer.
    private PubgRateLimitException toRateLimitException(HttpClientErrorException.TooManyRequests e) {
        HttpHeaders headers = e.getResponseHeaders();
        String retryAfterHeader = headers != null ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;

        Long retryAfterSeconds = null;
        if (retryAfterHeader != null) {
            try {
                retryAfterSeconds = Long.parseLong(retryAfterHeader);
            } catch (NumberFormatException ignored) {
                // Not a delay-seconds value - leave retryAfterSeconds null rather than guessing.
            }
        }

        return new PubgRateLimitException(retryAfterSeconds);
    }
}
