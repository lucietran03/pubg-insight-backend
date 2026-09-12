package com.pubginsight.player;

import com.pubginsight.client.pubg.dto.PubgGameModeStats;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgResourceIdentifier;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsAttributes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PlayerMapper {

    // Radar axes are scaled 0-100 against a fixed, documented ceiling rather than
    // against other players - this app has no population of other users' stats to
    // normalize against, so an absolute ceiling is the honest choice over a fake
    // percentile. Ceilings are rough "very strong player" reference points, not
    // statistically derived - if they ever feel off in a demo, adjust here only.
    private static final double COMBAT_KILLS_PER_ROUND_CEILING = 2.0;
    private static final double SURVIVAL_SECONDS_CEILING = 1200.0;
    private static final double PRECISION_HEADSHOT_RATE_CEILING = 0.5;
    private static final double AGGRESSION_DAMAGE_PER_ROUND_CEILING = 500.0;
    private static final double SUPPORT_PER_ROUND_CEILING = 1.0;

    // How far apart the highest and lowest axis score must be before a player is
    // considered to actually have a standout trait - below this, the profile reads as
    // "Balanced Operator" instead of picking one axis somewhat arbitrarily.
    private static final double BALANCED_SPREAD_THRESHOLD = 15.0;

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
        int totalKills = 0;
        int totalHeadshotKills = 0;
        int totalTop10s = 0;
        int totalAssists = 0;
        int totalRevives = 0;
        double totalDamage = 0.0;
        double totalTimeSurvived = 0.0;
        double longestKillMeters = 0.0;

        for (PubgGameModeStats m : attributes.gameModeStats().values()) {
            totalWins += orZero(m.wins());
            totalRounds += orZero(m.roundsPlayed());
            totalKills += orZero(m.kills());
            totalHeadshotKills += orZero(m.headshotKills());
            totalTop10s += orZero(m.top10s());
            totalAssists += orZero(m.assists());
            totalRevives += orZero(m.revives());
            totalDamage += orZero(m.damageDealt());
            totalTimeSurvived += orZero(m.timeSurvived());
            longestKillMeters = Math.max(longestKillMeters, orZero(m.longestKill()));
        }

        double winRate = totalRounds == 0 ? 0.0 : (double) totalWins / totalRounds;
        double avgDamage = totalRounds == 0 ? 0.0 : totalDamage / totalRounds;
        // PUBG's season stats don't expose a "deaths" field directly - rounds not won is
        // the standard community proxy for it (a round you didn't win is, for this
        // purpose, a round you effectively "died" in).
        double killDeathRatio = totalKills == 0 ? 0.0 : totalKills / (double) Math.max(1, totalRounds - totalWins);
        double headshotRate = totalKills == 0 ? 0.0 : (double) totalHeadshotKills / totalKills;
        double top10Rate = totalRounds == 0 ? 0.0 : (double) totalTop10s / totalRounds;
        double avgSurvivalSeconds = totalRounds == 0 ? 0.0 : totalTimeSurvived / totalRounds;
        double avgSupportPerRound = totalRounds == 0 ? 0.0 : (double) (totalAssists + totalRevives) / totalRounds;
        double combatKillsPerRound = totalRounds == 0 ? 0.0 : (double) totalKills / totalRounds;

        RadarScores radar = new RadarScores(
                scale(combatKillsPerRound, COMBAT_KILLS_PER_ROUND_CEILING),
                scale(avgSurvivalSeconds, SURVIVAL_SECONDS_CEILING),
                scale(headshotRate, PRECISION_HEADSHOT_RATE_CEILING),
                scale(avgDamage, AGGRESSION_DAMAGE_PER_ROUND_CEILING),
                scale(avgSupportPerRound, SUPPORT_PER_ROUND_CEILING),
                scale(top10Rate, 1.0)
        );

        return new SeasonStatsDto(
                totalWins,
                totalRounds,
                winRate,
                avgDamage,
                killDeathRatio,
                headshotRate,
                top10Rate,
                avgSurvivalSeconds,
                longestKillMeters,
                radar,
                classifyArchetype(radar),
                null
        );
    }

    // Percentage delta of each comparable metric between the current and previous season.
    // Guards against a zero previous-season value (e.g. a metric that never triggered,
    // such as a 0% headshot rate) by reporting 0% change instead of dividing by zero -
    // there's no meaningful "percent change" from a zero baseline.
    public SeasonComparison computeSeasonComparison(SeasonStatsDto current, SeasonStatsDto previous) {
        return new SeasonComparison(
                percentDelta(current.winRate(), previous.winRate()),
                percentDelta(current.avgDamage(), previous.avgDamage()),
                percentDelta(current.killDeathRatio(), previous.killDeathRatio()),
                percentDelta(current.headshotRate(), previous.headshotRate()),
                percentDelta(current.top10Rate(), previous.top10Rate())
        );
    }

    private static double percentDelta(double currentValue, double previousValue) {
        if (previousValue == 0.0) {
            return 0.0;
        }
        return ((currentValue - previousValue) / previousValue) * 100.0;
    }

    private static double scale(double value, double ceiling) {
        return Math.min(100.0, (value / ceiling) * 100.0);
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static double orZero(Double value) {
        return value == null ? 0.0 : value;
    }

    // Deterministic, rule-based classification from the radar axes - never generated by
    // Gemini (it's only asked to explain WHY a player got this label, see
    // InsightService), so the same stats always produce the same archetype.
    private String classifyArchetype(RadarScores radar) {
        Map<String, Double> axisScores = Map.of(
                "Frontline Eliminator", radar.combat(),
                "Survival Specialist", radar.survival(),
                "Precision Hunter", radar.precision(),
                "Aggressive Fragger", radar.aggression(),
                "Squad Anchor", radar.support(),
                "Consistent Competitor", radar.consistency()
        );

        double max = axisScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double min = axisScores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

        if (max - min < BALANCED_SPREAD_THRESHOLD) {
            return "Balanced Operator";
        }

        return axisScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Balanced Operator");
    }
}
