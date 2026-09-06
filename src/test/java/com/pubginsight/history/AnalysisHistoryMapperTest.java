package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.insight.InsightDto;
import com.pubginsight.match.MatchDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisHistoryMapperTest {

    private final AnalysisHistoryMapper mapper = new AnalysisHistoryMapper();

    @Test
    void mapsMatchAndInsightIntoItem() {
        MatchDto match = new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1);
        InsightDto insight = new InsightDto("Great match.", List.of("aim"), List.of("rotations"), List.of("play safer"));

        AnalysisHistoryItem item = mapper.toItem("account.1", "match-1", match, insight, "2026-09-06T00:00:00Z");

        assertThat(item.getPlayerId()).isEqualTo("account.1");
        assertThat(item.getMatchId()).isEqualTo("match-1");
        assertThat(item.getMapName()).isEqualTo("Erangel");
        assertThat(item.getGameMode()).isEqualTo("squad");
        assertThat(item.getKills()).isEqualTo(4);
        assertThat(item.getHeadshotRate()).isEqualTo(0.5);
        assertThat(item.getDamageDealt()).isEqualTo(520.0);
        assertThat(item.getTimeSurvivedSeconds()).isEqualTo(1200.0);
        assertThat(item.getWinPlace()).isEqualTo(1);
        assertThat(item.getInsightSummary()).isEqualTo("Great match.");
        assertThat(item.getStrengths()).containsExactly("aim");
        assertThat(item.getWeaknesses()).containsExactly("rotations");
        assertThat(item.getRecommendations()).containsExactly("play safer");
        assertThat(item.getCreatedAt()).isEqualTo("2026-09-06T00:00:00Z");
    }

    @Test
    void mapsItemBackToDto() {
        AnalysisHistoryItem item = new AnalysisHistoryItem("account.1", "match-1", "Erangel", "squad",
                4, 0.5, 520.0, 1200.0, 1, "Great match.",
                List.of("aim"), List.of("rotations"), List.of("play safer"), "2026-09-06T00:00:00Z");

        AnalysisHistoryDto dto = mapper.toDto(item);

        assertThat(dto.playerId()).isEqualTo("account.1");
        assertThat(dto.matchId()).isEqualTo("match-1");
        assertThat(dto.mapName()).isEqualTo("Erangel");
        assertThat(dto.gameMode()).isEqualTo("squad");
        assertThat(dto.kills()).isEqualTo(4);
        assertThat(dto.headshotRate()).isEqualTo(0.5);
        assertThat(dto.damageDealt()).isEqualTo(520.0);
        assertThat(dto.timeSurvivedSeconds()).isEqualTo(1200.0);
        assertThat(dto.winPlace()).isEqualTo(1);
        assertThat(dto.insightSummary()).isEqualTo("Great match.");
        assertThat(dto.strengths()).containsExactly("aim");
        assertThat(dto.weaknesses()).containsExactly("rotations");
        assertThat(dto.recommendations()).containsExactly("play safer");
        assertThat(dto.createdAt()).isEqualTo("2026-09-06T00:00:00Z");
    }

    @Test
    void defaultsNullNumericFieldsToZeroWhenMappingToDto() {
        AnalysisHistoryItem item = new AnalysisHistoryItem("account.1", "match-1", "Erangel", "squad",
                null, null, null, null, null, "Great match.",
                List.of(), List.of(), List.of(), "2026-09-06T00:00:00Z");

        AnalysisHistoryDto dto = mapper.toDto(item);

        assertThat(dto.kills()).isZero();
        assertThat(dto.headshotRate()).isZero();
        assertThat(dto.damageDealt()).isZero();
        assertThat(dto.timeSurvivedSeconds()).isZero();
        assertThat(dto.winPlace()).isZero();
    }
}
