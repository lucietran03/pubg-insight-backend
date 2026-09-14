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

    // Cached for the app's lifetime: seasons change every 2-3 months, so this saves a
    // PUBG call per season-stats request against the 10 req/min free-tier limit.
    private volatile SeasonIds cachedSeasonIds;

    // previousSeasonId is null when the current season is the account's first season.
    private record SeasonIds(String currentSeasonId, String previousSeasonId) {
    }

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
            // A read timeout while Spring is still reading the response body surfaces as a
            // plain RestClientException, not ResourceAccessException, so catch the supertype.
            throw new PubgApiException("PUBG API request failed for player '" + playerName + "'", e);
        }
    }

    public PubgPlayerListResponse findPlayerById(String accountId) {
        rateLimiter.acquire();
        try {
            return restClient.get()
                    .uri("/shards/{shard}/players?filter[playerIds]={id}", defaultShard, accountId)
                    .retrieve()
                    .body(PubgPlayerListResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return new PubgPlayerListResponse(List.of());
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            throw new PubgApiException("PUBG API request failed for account '" + accountId + "'", e);
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

    // Returns the raw JSON body because the telemetry asset URL in the response's
    // "included" array isn't modeled by PubgIncludedItem/PubgParticipantAttributes.
    public String findMatchRawJson(String matchId) {
        rateLimiter.acquire();
        try {
            return restClient.get()
                    .uri("/shards/{shard}/matches/{matchId}", defaultShard, matchId)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            throw new PubgApiException("PUBG API request failed for match '" + matchId + "' (raw)", e);
        }
    }

    public String findCurrentSeasonId() {
        return loadSeasonIds().currentSeasonId();
    }

    // Returns null when the current season has no season before it.
    public String findPreviousSeasonId() {
        return loadSeasonIds().previousSeasonId();
    }

    private SeasonIds loadSeasonIds() {
        SeasonIds cached = cachedSeasonIds;
        if (cached != null) {
            return cached;
        }

        rateLimiter.acquire();
        try {
            PubgSeasonListResponse response = restClient.get()
                    .uri("/shards/{shard}/seasons", defaultShard)
                    .retrieve()
                    .body(PubgSeasonListResponse.class);

            List<PubgSeasonData> seasons = response.data();
            int currentIndex = -1;
            for (int i = 0; i < seasons.size(); i++) {
                if (Boolean.TRUE.equals(seasons.get(i).attributes().isCurrentSeason())) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex == -1) {
                throw new PubgApiException("No current PUBG season found", null);
            }

            // Seasons have no explicit ordering field; PUBG returns them chronologically,
            // so the entry before the current one in the array is the previous season.
            String currentSeasonId = seasons.get(currentIndex).id();
            String previousSeasonId = currentIndex > 0 ? seasons.get(currentIndex - 1).id() : null;

            SeasonIds seasonIds = new SeasonIds(currentSeasonId, previousSeasonId);
            cachedSeasonIds = seasonIds;
            return seasonIds;
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
