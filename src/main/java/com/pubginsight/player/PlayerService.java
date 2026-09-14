package com.pubginsight.player;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.dynamodb.SeasonStatsCacheException;
import com.pubginsight.client.dynamodb.SeasonStatsCacheItem;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class PlayerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    // Season stats move slowly enough within an hour that a stale-by-up-to-an-hour read
    // is an acceptable tradeoff against PUBG's 10 req/min limit.
    private static final long CACHE_TTL_SECONDS = 3600;

    private final PubgApiClient pubgApiClient;
    private final PlayerMapper playerMapper;
    private final SeasonStatsCacheRepository seasonStatsCacheRepository;
    // Built directly rather than injected: this Spring Boot version auto-configures a
    // Jackson 3 JsonMapper bean, not a com.fasterxml.jackson.databind.ObjectMapper one.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlayerService(PubgApiClient pubgApiClient, PlayerMapper playerMapper,
                          SeasonStatsCacheRepository seasonStatsCacheRepository) {
        this.pubgApiClient = pubgApiClient;
        this.playerMapper = playerMapper;
        this.seasonStatsCacheRepository = seasonStatsCacheRepository;
    }

    public PlayerDto searchPlayerByName(String playerName) {
        PubgPlayerListResponse response = pubgApiClient.findPlayerByName(playerName);

        if (response.data() == null || response.data().isEmpty()) {
            throw new PlayerNotFoundException(playerName);
        }

        return playerMapper.toPlayerDto(response.data().get(0));
    }

    // Used to resolve a share link's bare account id back into a displayable player -
    // the share flow only stores accountId, never the display name.
    public PlayerDto findPlayerById(String accountId) {
        PubgPlayerListResponse response = pubgApiClient.findPlayerById(accountId);

        if (response.data() == null || response.data().isEmpty()) {
            throw new PlayerNotFoundException(accountId);
        }

        return playerMapper.toPlayerDto(response.data().get(0));
    }

    public SeasonStatsDto getSeasonStats(String accountId) {
        SeasonStatsDto cached = readFromCache(accountId);
        if (cached != null) {
            return cached;
        }

        SeasonStatsDto fresh = fetchSeasonStatsFromPubg(accountId);
        writeToCache(accountId, fresh);
        return fresh;
    }

    private SeasonStatsDto fetchSeasonStatsFromPubg(String accountId) {
        String seasonId = pubgApiClient.findCurrentSeasonId();
        PubgSeasonStatsResponse response = pubgApiClient.findSeasonStats(accountId, seasonId);

        if (response == null) {
            throw new PlayerNotFoundException(accountId);
        }

        SeasonStatsDto currentSeasonStats = playerMapper.toSeasonStatsDto(response.data().attributes());

        SeasonComparison comparison = tryComputePreviousSeasonComparison(accountId, currentSeasonStats);
        return currentSeasonStats.withPreviousSeasonComparison(comparison);
    }

    // Cache misses/failures fall through to a live PUBG call rather than failing the
    // request - a broken cache must never break the feature it's speeding up.
    private SeasonStatsDto readFromCache(String accountId) {
        try {
            Optional<SeasonStatsCacheItem> item = seasonStatsCacheRepository.findByPlayerId(accountId);
            if (item.isEmpty() || item.get().getExpiresAt() < Instant.now().getEpochSecond()) {
                return null;
            }
            return objectMapper.readValue(item.get().getJson(), SeasonStatsDto.class);
        } catch (SeasonStatsCacheException e) {
            log.warn("Season-stats cache read failed for player '{}', falling back to PUBG API", accountId, e);
            return null;
        } catch (JsonProcessingException e) {
            log.warn("Cached season stats for player '{}' could not be deserialized, falling back to PUBG API", accountId, e);
            return null;
        }
    }

    private void writeToCache(String accountId, SeasonStatsDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            long expiresAt = Instant.now().getEpochSecond() + CACHE_TTL_SECONDS;
            seasonStatsCacheRepository.save(new SeasonStatsCacheItem(accountId, json, expiresAt));
        } catch (SeasonStatsCacheException e) {
            log.warn("Season-stats cache write failed for player '{}' - continuing without caching", accountId, e);
        } catch (JsonProcessingException e) {
            log.warn("Season stats for player '{}' could not be serialized for caching - continuing without caching", accountId, e);
        }
    }

    // Returns null when there's no previous season to compare against - a normal state,
    // not an error.
    private SeasonComparison tryComputePreviousSeasonComparison(String accountId, SeasonStatsDto currentSeasonStats) {
        String previousSeasonId = pubgApiClient.findPreviousSeasonId();
        if (previousSeasonId == null) {
            return null;
        }

        PubgSeasonStatsResponse previousResponse = pubgApiClient.findSeasonStats(accountId, previousSeasonId);
        if (previousResponse == null) {
            return null;
        }

        SeasonStatsDto previousSeasonStats = playerMapper.toSeasonStatsDto(previousResponse.data().attributes());
        return playerMapper.computeSeasonComparison(currentSeasonStats, previousSeasonStats);
    }
}
