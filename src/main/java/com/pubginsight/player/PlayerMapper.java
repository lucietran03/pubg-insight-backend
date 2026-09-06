package com.pubginsight.player;

import com.pubginsight.client.pubg.dto.PubgGameModeStats;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgResourceIdentifier;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsAttributes;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerMapper {

    public PlayerDto toPlayerDto(PubgPlayerData data) {
        List<String> matchIds = data.relationships() == null || data.relationships().matches() == null
                ? List.of()
                : data.relationships().matches().data().stream()
                        .map(PubgResourceIdentifier::id)
                        .toList();

        return new PlayerDto(data.id(), data.attributes().name(), data.attributes().shardId(), matchIds);
    }

    public SeasonStatsDto toSeasonStatsDto(PubgSeasonStatsAttributes attributes) {
        int totalWins = 0;
        int totalRounds = 0;

        for (PubgGameModeStats modeStats : attributes.gameModeStats().values()) {
            totalWins += modeStats.wins() == null ? 0 : modeStats.wins();
            totalRounds += modeStats.roundsPlayed() == null ? 0 : modeStats.roundsPlayed();
        }

        double winRate = totalRounds == 0 ? 0.0 : (double) totalWins / totalRounds;

        return new SeasonStatsDto(totalWins, totalRounds, winRate);
    }
}
