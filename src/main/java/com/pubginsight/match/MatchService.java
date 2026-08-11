package com.pubginsight.match;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgIncludedItem;
import com.pubginsight.client.pubg.dto.PubgMatchResponse;
import com.pubginsight.client.pubg.dto.PubgParticipantAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MatchService {

    private final PubgApiClient pubgApiClient;
    private final MatchMapper matchMapper;

    public MatchService(PubgApiClient pubgApiClient, MatchMapper matchMapper) {
        this.pubgApiClient = pubgApiClient;
        this.matchMapper = matchMapper;
    }

    public MatchDto getMatchStatsForPlayer(String matchId, String playerId) {
        PubgMatchResponse response = pubgApiClient.findMatchById(matchId);

        if (response == null) {
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
}
