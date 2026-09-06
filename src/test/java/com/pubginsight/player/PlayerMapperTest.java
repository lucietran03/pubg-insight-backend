package com.pubginsight.player;

import com.pubginsight.client.pubg.dto.PubgPlayerAttributes;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgPlayerRelationships;
import com.pubginsight.client.pubg.dto.PubgRelationshipData;
import com.pubginsight.client.pubg.dto.PubgResourceIdentifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerMapperTest {

    private final PlayerMapper mapper = new PlayerMapper();

    @Test
    void mapsBasicFieldsAndFlattensMatchIds() {
        PubgPlayerData data = new PubgPlayerData(
                "player",
                "account.123",
                new PubgPlayerAttributes("shroud", "steam", "pubg"),
                new PubgPlayerRelationships(new PubgRelationshipData(List.of(
                        new PubgResourceIdentifier("match", "match-1"),
                        new PubgResourceIdentifier("match", "match-2")
                )))
        );

        PlayerDto dto = mapper.toPlayerDto(data);

        assertThat(dto.id()).isEqualTo("account.123");
        assertThat(dto.name()).isEqualTo("shroud");
        assertThat(dto.shardId()).isEqualTo("steam");
        assertThat(dto.recentMatchIds()).containsExactly("match-1", "match-2");
    }

    @Test
    void returnsEmptyMatchIdsWhenRelationshipsMissing() {
        PubgPlayerData data = new PubgPlayerData(
                "player", "account.456", new PubgPlayerAttributes("noMatches", "steam", "pubg"), null
        );

        PlayerDto dto = mapper.toPlayerDto(data);

        assertThat(dto.recentMatchIds()).isEmpty();
    }
}
