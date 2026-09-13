package com.pubginsight.insight;

import com.pubginsight.match.MatchDto;
import com.pubginsight.player.SeasonStatsDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Deterministic fallback used only when Gemini is unavailable; output is always
// labelled InsightDto.source() = "offline" so it's never mistaken for a real AI result.
@Component
public class OfflineInsightWriter {

    private static final double NOTABLY_HIGH_HEADSHOT_RATE = 0.4;
    private static final double NOTABLY_LOW_SURVIVAL_SECONDS = 600;
    private static final int NOTABLY_HIGH_KILLS = 5;

    public InsightDto write(MatchDto match, SeasonStatsDto seasonStats) {
        String summary = "You placed #%d in a %s match on %s with %d kills and %.0f damage dealt, surviving %.0f seconds. Season win rate: %.1f%% over %d rounds."
                .formatted(match.winPlace(), match.gameMode(), match.mapName(), match.kills(),
                        match.damageDealt(), match.timeSurvivedSeconds(),
                        seasonStats.winRate() * 100, seasonStats.roundsPlayed());

        List<String> strengths = new ArrayList<>();
        if (match.headshotRate() >= NOTABLY_HIGH_HEADSHOT_RATE) {
            strengths.add("high headshot accuracy");
        }
        if (match.kills() >= NOTABLY_HIGH_KILLS) {
            strengths.add("strong kill count");
        }

        List<String> weaknesses = new ArrayList<>();
        if (match.timeSurvivedSeconds() <= NOTABLY_LOW_SURVIVAL_SECONDS) {
            weaknesses.add("short survival time");
        }
        if (match.kills() == 0) {
            weaknesses.add("no eliminations this match");
        }

        List<String> recommendations = new ArrayList<>();
        if (match.timeSurvivedSeconds() <= NOTABLY_LOW_SURVIVAL_SECONDS) {
            recommendations.add("play more cautiously early to survive longer");
        }
        if (match.headshotRate() < NOTABLY_HIGH_HEADSHOT_RATE) {
            recommendations.add("practice aim to improve headshot rate");
        }

        // No Gemini call to draw from, so the coach sections are left empty rather than guessed at.
        return new InsightDto(summary, strengths, weaknesses, recommendations,
                "", "", List.of(), List.of(), "offline");
    }
}
