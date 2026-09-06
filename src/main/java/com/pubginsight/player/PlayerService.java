package com.pubginsight.player;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsResponse;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final PubgApiClient pubgApiClient;
    private final PlayerMapper playerMapper;

    public PlayerService(PubgApiClient pubgApiClient, PlayerMapper playerMapper) {
        this.pubgApiClient = pubgApiClient;
        this.playerMapper = playerMapper;
    }

    public PlayerDto searchPlayerByName(String playerName) {
        PubgPlayerListResponse response = pubgApiClient.findPlayerByName(playerName);

        if (response.data() == null || response.data().isEmpty()) {
            throw new PlayerNotFoundException(playerName);
        }

        return playerMapper.toPlayerDto(response.data().get(0));
    }

    public SeasonStatsDto getSeasonStats(String accountId) {
        String seasonId = pubgApiClient.findCurrentSeasonId();
        PubgSeasonStatsResponse response = pubgApiClient.findSeasonStats(accountId, seasonId);

        if (response == null) {
            throw new PlayerNotFoundException(accountId);
        }

        return playerMapper.toSeasonStatsDto(response.data().attributes());
    }
}
