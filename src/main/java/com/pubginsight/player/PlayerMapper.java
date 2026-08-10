package com.pubginsight.player;

import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgResourceIdentifier;
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
}
