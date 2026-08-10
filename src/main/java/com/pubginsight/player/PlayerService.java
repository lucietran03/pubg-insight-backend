package com.pubginsight.player;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
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
}
