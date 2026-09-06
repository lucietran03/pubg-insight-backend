package com.pubginsight.match;

import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchMapperTest {

    private final MatchMapper mapper = new MatchMapper();

    @Test
    void computesHeadshotRateCorrectly() {
        PubgMatchAttributes matchAttributes = new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official");
        PubgParticipantStats stats = new PubgParticipantStats(
                "account.1", "shroud", 4, 2, 520.0, 1200.0, 1, 1, 0
        );

        MatchDto dto = mapper.toMatchDto("match-1", matchAttributes, stats);

        assertThat(dto.kills()).isEqualTo(4);
        assertThat(dto.headshotKills()).isEqualTo(2);
        assertThat(dto.headshotRate()).isEqualTo(0.5);
        assertThat(dto.winPlace()).isEqualTo(1);
    }

    @Test
    void avoidsDivisionByZeroWhenNoKills() {
        PubgMatchAttributes matchAttributes = new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official");
        PubgParticipantStats stats = new PubgParticipantStats(
                "account.1", "newPlayer", 0, 0, 0.0, 300.0, 50, 50, 0
        );

        MatchDto dto = mapper.toMatchDto("match-1", matchAttributes, stats);

        assertThat(dto.headshotRate()).isEqualTo(0.0);
    }

    @Test
    void defaultsNullNumericFieldsToZero() {
        PubgMatchAttributes matchAttributes = new PubgMatchAttributes(null, 1800, "squad", "Erangel", "official");
        PubgParticipantStats stats = new PubgParticipantStats(
                "account.1", "player", null, null, null, null, null, null, null
        );

        MatchDto dto = mapper.toMatchDto("match-1", matchAttributes, stats);

        assertThat(dto.kills()).isZero();
        assertThat(dto.damageDealt()).isZero();
        assertThat(dto.timeSurvivedSeconds()).isZero();
        assertThat(dto.winPlace()).isZero();
    }
}
