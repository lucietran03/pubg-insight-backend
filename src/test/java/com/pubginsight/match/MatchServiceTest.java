package com.pubginsight.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgIncludedItem;
import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgMatchData;
import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgParticipantAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import com.pubginsight.client.s3.S3CacheException;
import com.pubginsight.client.s3.S3MatchCacheClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private PubgApiClient pubgApiClient;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private S3MatchCacheClient s3MatchCacheClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MatchService matchService;

    @Test
    void throwsMatchNotFoundWhenMatchDoesNotExist() {
        when(s3MatchCacheClient.getCachedMatchJson("missing-match")).thenReturn(Optional.empty());
        when(pubgApiClient.findMatchById("missing-match")).thenReturn(null);

        assertThatThrownBy(() -> matchService.getMatchStatsForPlayer("missing-match", "account.1"))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void throwsMatchNotFoundWhenPlayerDidNotParticipate() {
        PubgMatchData matchData = new PubgMatchData("match", "match-1",
                new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official"));
        PubgIncludedItem otherPlayerParticipant = new PubgIncludedItem("participant", "p-2",
                new PubgParticipantAttributes(new PubgParticipantStats(
                        "account.other", "someoneElse", 1, 0, 100.0, 500.0, 10, 10, 0)));
        PubgMatchResponse response = new PubgMatchResponse(matchData, List.of(otherPlayerParticipant));

        when(s3MatchCacheClient.getCachedMatchJson("match-1")).thenReturn(Optional.empty());
        when(pubgApiClient.findMatchById("match-1")).thenReturn(response);

        assertThatThrownBy(() -> matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void cacheHitSkipsPubgCallAndReturnsMappedDto() throws Exception {
        PubgMatchResponse response = matchResponseFor("account.1");
        MatchDto expectedDto = new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1);
        String cachedJson = "{\"cached\":true}";

        when(s3MatchCacheClient.getCachedMatchJson("match-1")).thenReturn(Optional.of(cachedJson));
        when(objectMapper.readValue(cachedJson, PubgMatchResponse.class)).thenReturn(response);
        when(matchMapper.toMatchDto(eq("match-1"), any(), any())).thenReturn(expectedDto);

        MatchDto actual = matchService.getMatchStatsForPlayer("match-1", "account.1");

        assertThat(actual).isEqualTo(expectedDto);
        verify(pubgApiClient, never()).findMatchById(any());
        // Cache hit must never re-write the cache - it's already there.
        verify(s3MatchCacheClient, never()).cacheMatchJson(any(), any());
    }

    @Test
    void cacheMissCallsPubgAndCachesTheResult() throws Exception {
        PubgMatchResponse response = matchResponseFor("account.1");
        MatchDto expectedDto = new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1);
        String serializedJson = "{\"serialized\":true}";

        when(s3MatchCacheClient.getCachedMatchJson("match-1")).thenReturn(Optional.empty());
        when(pubgApiClient.findMatchById("match-1")).thenReturn(response);
        when(objectMapper.writeValueAsString(response)).thenReturn(serializedJson);
        when(matchMapper.toMatchDto(eq("match-1"), any(), any())).thenReturn(expectedDto);

        MatchDto actual = matchService.getMatchStatsForPlayer("match-1", "account.1");

        assertThat(actual).isEqualTo(expectedDto);
        verify(pubgApiClient, times(1)).findMatchById("match-1");
        verify(s3MatchCacheClient, times(1)).cacheMatchJson("match-1", serializedJson);
    }

    @Test
    void fallsThroughToPubgWhenCacheReadFails() {
        PubgMatchResponse response = matchResponseFor("account.1");
        MatchDto expectedDto = new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1);

        when(s3MatchCacheClient.getCachedMatchJson("match-1"))
                .thenThrow(new S3CacheException("bucket not reachable", new RuntimeException("boom")));
        when(pubgApiClient.findMatchById("match-1")).thenReturn(response);
        when(matchMapper.toMatchDto(eq("match-1"), any(), any())).thenReturn(expectedDto);

        MatchDto actual = matchService.getMatchStatsForPlayer("match-1", "account.1");

        assertThat(actual).isEqualTo(expectedDto);
        verify(pubgApiClient, times(1)).findMatchById("match-1");
    }

    @Test
    void fallsThroughSilentlyWhenCacheWriteFails() throws Exception {
        PubgMatchResponse response = matchResponseFor("account.1");
        MatchDto expectedDto = new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1);

        when(s3MatchCacheClient.getCachedMatchJson("match-1")).thenReturn(Optional.empty());
        when(pubgApiClient.findMatchById("match-1")).thenReturn(response);
        when(objectMapper.writeValueAsString(response)).thenReturn("{}");
        doThrow(new S3CacheException("bucket not reachable", new RuntimeException("boom")))
                .when(s3MatchCacheClient).cacheMatchJson(anyString(), anyString());
        when(matchMapper.toMatchDto(eq("match-1"), any(), any())).thenReturn(expectedDto);

        MatchDto actual = matchService.getMatchStatsForPlayer("match-1", "account.1");

        assertThat(actual).isEqualTo(expectedDto);
    }

    private static PubgMatchResponse matchResponseFor(String playerId) {
        PubgMatchData matchData = new PubgMatchData("match", "match-1",
                new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official"));
        PubgIncludedItem participant = new PubgIncludedItem("participant", "p-1",
                new PubgParticipantAttributes(new PubgParticipantStats(
                        playerId, "shroud", 4, 2, 520.0, 1200.0, 1, 1, 0)));
        return new PubgMatchResponse(matchData, List.of(participant));
    }
}
