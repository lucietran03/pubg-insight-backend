package com.pubginsight.match;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgIncludedItem;
import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgMatchData;
import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgParticipantAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private PubgApiClient pubgApiClient;

    @Mock
    private MatchMapper matchMapper;

    @InjectMocks
    private MatchService matchService;

    @Test
    void throwsMatchNotFoundWhenMatchDoesNotExist() {
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

        when(pubgApiClient.findMatchById("match-1")).thenReturn(response);

        assertThatThrownBy(() -> matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .isInstanceOf(MatchNotFoundException.class);
    }
}
