package com.pubginsight.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgIncludedItem;
import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgParticipantAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import com.pubginsight.client.s3.S3CacheException;
import com.pubginsight.client.s3.S3MatchCacheClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final PubgApiClient pubgApiClient;
    private final MatchMapper matchMapper;
    private final S3MatchCacheClient s3MatchCacheClient;
    // Built directly rather than injected: this Spring Boot version auto-configures a
    // Jackson 3 JsonMapper bean, not a com.fasterxml.jackson.databind.ObjectMapper one.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchService(PubgApiClient pubgApiClient, MatchMapper matchMapper,
                         S3MatchCacheClient s3MatchCacheClient) {
        this.pubgApiClient = pubgApiClient;
        this.matchMapper = matchMapper;
        this.s3MatchCacheClient = s3MatchCacheClient;
    }

    public MatchDto getMatchStatsForPlayer(String matchId, String playerId) {
        PubgMatchResponse response = fetchFromCache(matchId);

        if (response == null) {
            response = pubgApiClient.findMatchById(matchId);
            cacheIfPresent(matchId, response);
        }

        if (response == null || response.included() == null) {
            throw new MatchNotFoundException("Match '" + matchId + "' not found");
        }

        PubgParticipantStats stats = response.included().stream()
                .filter(item -> "participant".equals(item.type()))
                .map(PubgIncludedItem::attributes)
                .filter(Objects::nonNull)
                .map(PubgParticipantAttributes::stats)
                .filter(Objects::nonNull)
                .filter(s -> playerId.equals(s.playerId()))
                .findFirst()
                .orElseThrow(() -> new MatchNotFoundException(
                        "Player '" + playerId + "' not found in match '" + matchId + "'"));

        return matchMapper.toMatchDto(matchId, response.data().attributes(), stats);
    }

    // A completed match is immutable, so a cache hit avoids a PUBG API call entirely -
    // important given PUBG's 10 req/min free-tier limit. Cache failures are soft-failed
    // (return null, same as a miss) so a broken cache never breaks the feature.
    private PubgMatchResponse fetchFromCache(String matchId) {
        try {
            Optional<String> cachedJson = s3MatchCacheClient.getCachedMatchJson(matchId);
            if (cachedJson.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(cachedJson.get(), PubgMatchResponse.class);
        } catch (S3CacheException e) {
            log.warn("S3 match cache read failed for match '{}', falling back to PUBG API", matchId, e);
            return null;
        } catch (JsonProcessingException e) {
            log.warn("Cached JSON for match '{}' could not be deserialized, falling back to PUBG API", matchId, e);
            return null;
        }
    }

    // Write failures are soft-failed too: the match was already fetched successfully,
    // so a caching error must not fail the request.
    private void cacheIfPresent(String matchId, PubgMatchResponse response) {
        if (response == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            s3MatchCacheClient.cacheMatchJson(matchId, json);
        } catch (S3CacheException e) {
            log.warn("S3 match cache write failed for match '{}' - continuing without caching", matchId, e);
        } catch (JsonProcessingException e) {
            log.warn("Match '{}' response could not be serialized for caching - continuing without caching", matchId, e);
        }
    }
}
