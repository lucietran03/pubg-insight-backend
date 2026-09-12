package com.pubginsight.player;

import com.pubginsight.client.pubg.dto.PubgGameModeStats;
import com.pubginsight.client.pubg.dto.PubgPlayerAttributes;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgPlayerRelationships;
import com.pubginsight.client.pubg.dto.PubgRelationshipData;
import com.pubginsight.client.pubg.dto.PubgResourceIdentifier;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsAttributes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

    @Test
    void aggregatesSeasonStatsAcrossGameModesAndClassifiesArchetype() {
        // Sums to: wins=7, rounds=70, kills=75, headshot=25, damage=18000, timeSurvived=33000,
        // top10s=25, assists=10, revives=5, longestKill=max(120,80)=120 - chosen so precision
        // (headshot rate 25/75=33%) is clearly the standout axis over the others.
        PubgGameModeStats squad = new PubgGameModeStats(
                5, 50, 45, 60, 10, 20, 15000.0, 25000.0, 900.0, 120.0, 20, 5, 0, 0, 0, 0, 0.0, 0.0, 0, 0);
        PubgGameModeStats solo = new PubgGameModeStats(
                2, 20, 18, 15, 0, 5, 3000.0, 8000.0, 400.0, 80.0, 5, 0, 0, 0, 0, 0, 0.0, 0.0, 0, 0);
        PubgSeasonStatsAttributes attributes = new PubgSeasonStatsAttributes(Map.of("squad", squad, "solo", solo));

        SeasonStatsDto dto = mapper.toSeasonStatsDto(attributes);

        assertThat(dto.wins()).isEqualTo(7);
        assertThat(dto.roundsPlayed()).isEqualTo(70);
        assertThat(dto.winRate()).isCloseTo(0.1, within(0.0001));
        assertThat(dto.avgDamage()).isCloseTo(257.14, within(0.01));
        assertThat(dto.killDeathRatio()).isCloseTo(1.1905, within(0.001));
        assertThat(dto.headshotRate()).isCloseTo(0.3333, within(0.001));
        assertThat(dto.top10Rate()).isCloseTo(0.3571, within(0.001));
        assertThat(dto.longestKillMeters()).isEqualTo(120.0);

        // Precision (headshot rate 33% against a 50% ceiling) is the standout axis.
        assertThat(dto.radar().precision()).isGreaterThan(dto.radar().combat());
        assertThat(dto.radar().precision()).isGreaterThan(dto.radar().support());
        assertThat(dto.archetype()).isEqualTo("Precision Hunter");
    }

    @Test
    void classifiesFlatProfileAsBalancedOperator() {
        // Every axis lands on exactly 50 against its own ceiling (see PlayerMapper's
        // scale() constants) - kills/round=1.0, damage/round=250, headshot rate=25%,
        // survival/round=600s, support/round=0.5, top10 rate=50%.
        PubgGameModeStats squad = new PubgGameModeStats(
                10, 100, 90, 100, 30, 25, 25000.0, 60000.0, 1000.0, 100.0, 50, 20, 0, 0, 0, 0, 0.0, 0.0, 0, 0);
        PubgSeasonStatsAttributes attributes = new PubgSeasonStatsAttributes(Map.of("squad", squad));

        SeasonStatsDto dto = mapper.toSeasonStatsDto(attributes);

        assertThat(dto.radar().combat()).isCloseTo(50.0, within(0.01));
        assertThat(dto.radar().survival()).isCloseTo(50.0, within(0.01));
        assertThat(dto.radar().precision()).isCloseTo(50.0, within(0.01));
        assertThat(dto.radar().aggression()).isCloseTo(50.0, within(0.01));
        assertThat(dto.radar().support()).isCloseTo(50.0, within(0.01));
        assertThat(dto.radar().consistency()).isCloseTo(50.0, within(0.01));
        assertThat(dto.archetype()).isEqualTo("Balanced Operator");
    }

    @Test
    void avoidsDivisionByZeroWhenNoRoundsPlayed() {
        PubgGameModeStats empty = new PubgGameModeStats(
                0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0, 0);
        PubgSeasonStatsAttributes attributes = new PubgSeasonStatsAttributes(Map.of("squad", empty));

        SeasonStatsDto dto = mapper.toSeasonStatsDto(attributes);

        assertThat(dto.winRate()).isZero();
        assertThat(dto.avgDamage()).isZero();
        assertThat(dto.killDeathRatio()).isZero();
        assertThat(dto.headshotRate()).isZero();
        assertThat(dto.archetype()).isEqualTo("Balanced Operator");
    }

    @Test
    void toSeasonStatsDtoLeavesPreviousSeasonComparisonNull() {
        // toSeasonStatsDto() maps a single season in isolation - it has no visibility into
        // any other season, so previousSeasonComparison always starts null here. It's
        // PlayerService's job (it's the one holding both seasons) to attach a comparison
        // via withPreviousSeasonComparison() afterwards.
        PubgGameModeStats squad = new PubgGameModeStats(
                5, 50, 45, 60, 10, 20, 15000.0, 25000.0, 900.0, 120.0, 20, 5, 0, 0, 0, 0, 0.0, 0.0, 0, 0);
        PubgSeasonStatsAttributes attributes = new PubgSeasonStatsAttributes(Map.of("squad", squad));

        SeasonStatsDto dto = mapper.toSeasonStatsDto(attributes);

        assertThat(dto.previousSeasonComparison()).isNull();
    }

    @Test
    void computesSeasonComparisonAsPercentageDeltas() {
        SeasonStatsDto current = seasonStats(0.2, 300.0, 2.0, 0.4, 0.5);
        SeasonStatsDto previous = seasonStats(0.1, 250.0, 1.0, 0.5, 0.25);

        SeasonComparison comparison = mapper.computeSeasonComparison(current, previous);

        assertThat(comparison.winRateDeltaPct()).isCloseTo(100.0, within(0.001));
        assertThat(comparison.avgDamageDeltaPct()).isCloseTo(20.0, within(0.001));
        assertThat(comparison.killDeathRatioDeltaPct()).isCloseTo(100.0, within(0.001));
        assertThat(comparison.headshotRateDeltaPct()).isCloseTo(-20.0, within(0.001));
        assertThat(comparison.top10RateDeltaPct()).isCloseTo(100.0, within(0.001));
    }

    @Test
    void computeSeasonComparisonGuardsAgainstZeroPreviousSeasonMetrics() {
        // Every previous-season metric is 0 - there is no meaningful "percent change" from
        // a zero baseline, so each delta should come back as 0% rather than NaN/Infinity.
        SeasonStatsDto current = seasonStats(0.2, 300.0, 2.0, 0.4, 0.5);
        SeasonStatsDto previous = seasonStats(0.0, 0.0, 0.0, 0.0, 0.0);

        SeasonComparison comparison = mapper.computeSeasonComparison(current, previous);

        assertThat(comparison.winRateDeltaPct()).isZero();
        assertThat(comparison.avgDamageDeltaPct()).isZero();
        assertThat(comparison.killDeathRatioDeltaPct()).isZero();
        assertThat(comparison.headshotRateDeltaPct()).isZero();
        assertThat(comparison.top10RateDeltaPct()).isZero();
    }

    private static SeasonStatsDto seasonStats(
            double winRate, double avgDamage, double killDeathRatio, double headshotRate, double top10Rate) {
        return new SeasonStatsDto(
                0, 0, winRate, avgDamage, killDeathRatio, headshotRate, top10Rate, 0.0, 0.0,
                new RadarScores(0, 0, 0, 0, 0, 0), "Balanced Operator", null);
    }
}
