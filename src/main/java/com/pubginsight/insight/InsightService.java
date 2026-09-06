package com.pubginsight.insight;

import com.pubginsight.client.gemini.GeminiApiClient;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import com.pubginsight.player.PlayerService;
import com.pubginsight.player.SeasonStatsDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Orchestrates Player + Match features to gather already-aggregated metrics, then asks
// Gemini to turn them into a natural-language summary. Gemini never sees raw match
// telemetry - only the numbers PlayerService/MatchService already computed.
@Service
public class InsightService {

    private final PlayerService playerService;
    private final MatchService matchService;
    private final GeminiApiClient geminiApiClient;

    public InsightService(PlayerService playerService, MatchService matchService, GeminiApiClient geminiApiClient) {
        this.playerService = playerService;
        this.matchService = matchService;
        this.geminiApiClient = geminiApiClient;
    }

    public InsightDto generateInsights(String playerId, String matchId) {
        SeasonStatsDto seasonStats = playerService.getSeasonStats(playerId);
        MatchDto match = matchService.getMatchStatsForPlayer(matchId, playerId);

        String rawText = geminiApiClient.generateText(buildPrompt(match, seasonStats));
        return parseInsight(rawText);
    }

    private String buildPrompt(MatchDto match, SeasonStatsDto seasonStats) {
        return """
                You are a PUBG performance coach. Based ONLY on the aggregated stats below \
                (you are not given raw match telemetry), respond in EXACTLY this format, one \
                section per line:

                SUMMARY: <one short paragraph>
                STRENGTHS: <comma-separated list>
                WEAKNESSES: <comma-separated list>
                RECOMMENDATIONS: <comma-separated list>

                Match stats:
                - Map: %s (%s)
                - Placement: #%d
                - Kills: %d
                - Headshot rate: %.0f%%
                - Damage dealt: %.0f
                - Survived: %.0f seconds

                Season context (all game modes combined):
                - Win rate: %.1f%% (%d wins / %d rounds played)
                """.formatted(
                match.mapName(), match.gameMode(), match.winPlace(), match.kills(),
                match.headshotRate() * 100, match.damageDealt(), match.timeSurvivedSeconds(),
                seasonStats.winRate() * 100, seasonStats.wins(), seasonStats.roundsPlayed());
    }

    private InsightDto parseInsight(String rawText) {
        String summary = extractSection(rawText, "SUMMARY");
        if (summary.isEmpty()) {
            // Gemini didn't follow the requested format - fall back to showing the raw
            // text as the summary rather than failing the whole request.
            return new InsightDto(rawText.trim(), List.of(), List.of(), List.of());
        }

        return new InsightDto(
                summary,
                extractList(rawText, "STRENGTHS"),
                extractList(rawText, "WEAKNESSES"),
                extractList(rawText, "RECOMMENDATIONS"));
    }

    private String extractSection(String text, String label) {
        Matcher matcher = Pattern.compile(label + ":\\s*(.+)").matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private List<String> extractList(String text, String label) {
        String section = extractSection(text, label);
        if (section.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(section.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
