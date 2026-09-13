package com.pubginsight.insight;

import com.pubginsight.client.gemini.GeminiApiClient;
import com.pubginsight.client.gemini.GeminiApiException;
import com.pubginsight.client.gemini.GeminiRateLimitException;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import com.pubginsight.player.PlayerService;
import com.pubginsight.player.RadarScores;
import com.pubginsight.player.SeasonStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Gemini never sees raw match telemetry, only the aggregated metrics from PlayerService/MatchService.
@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);
    private static final String GEMINI_SOURCE = "gemini";

    private final PlayerService playerService;
    private final MatchService matchService;
    private final GeminiApiClient geminiApiClient;
    private final OfflineInsightWriter offlineInsightWriter;

    // A given (player, match) insight never changes, so an in-memory cache avoids
    // re-billing the Gemini API on repeat views.
    private final ConcurrentHashMap<String, InsightDto> insightCache = new ConcurrentHashMap<>();

    public InsightService(PlayerService playerService, MatchService matchService,
                           GeminiApiClient geminiApiClient, OfflineInsightWriter offlineInsightWriter) {
        this.playerService = playerService;
        this.matchService = matchService;
        this.geminiApiClient = geminiApiClient;
        this.offlineInsightWriter = offlineInsightWriter;
    }

    public InsightDto generateInsights(String playerId, String matchId) {
        String cacheKey = playerId + ":" + matchId;
        InsightDto cached = insightCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        SeasonStatsDto seasonStats = playerService.getSeasonStats(playerId);
        MatchDto match = matchService.getMatchStatsForPlayer(matchId, playerId);

        InsightDto insight;
        try {
            String rawText = geminiApiClient.generateText(buildPrompt(match, seasonStats));
            insight = parseInsight(rawText);
        } catch (GeminiApiException | GeminiRateLimitException e) {
            // Not cached, unlike a genuine Gemini result: retry on the next request in case Gemini recovers.
            log.warn("Gemini unavailable for player '{}' match '{}' - returning offline fallback insight",
                    playerId, matchId, e);
            return offlineInsightWriter.write(match, seasonStats);
        }

        insightCache.put(cacheKey, insight);
        return insight;
    }

    private String buildPrompt(MatchDto match, SeasonStatsDto seasonStats) {
        RadarScores radar = seasonStats.radar();
        String damageDelta = describeDeltaVsSeasonAverage(match.damageDealt(), seasonStats.avgDamage());
        String survivalDelta = describeDeltaVsSeasonAverage(match.timeSurvivedSeconds(), seasonStats.avgSurvivalSeconds());
        String headshotRateDelta = describeDeltaVsSeasonAverage(match.headshotRate(), seasonStats.headshotRate());

        return """
                You are a PUBG performance coach. Based ONLY on the aggregated stats below \
                (you are not given raw match telemetry), respond in EXACTLY this format, one \
                section per line:

                SUMMARY: <one short paragraph>
                STRENGTHS: <comma-separated list>
                WEAKNESSES: <comma-separated list>
                RECOMMENDATIONS: <comma-separated list>
                PLAYSTYLE: <one short paragraph explaining how this player's archetype and radar shape reflect the way they play>
                SEASON_PROGRESS: <one short paragraph commenting on their season aggregate stats>
                RISK_FACTORS: <comma-separated list of things that could be going wrong, drawn from weak radar axes or negative deltas below>
                TRAINING_PRIORITIES: <comma-separated list of concrete next-focus areas>

                Match stats:
                - Map: %s (%s)
                - Placement: #%d
                - Kills: %d
                - Headshot rate: %.0f%%
                - Damage dealt: %.0f
                - Survived: %.0f seconds

                This match compared with this player's season average:
                - Damage dealt: %s
                - Survival time: %s
                - Headshot rate: %s

                Season context (all game modes combined):
                - Win rate: %.1f%% (%d wins / %d rounds played)
                - Average damage per match: %.0f
                - Kill/death ratio: %.2f
                - Headshot rate: %.0f%%
                - Top 10 rate: %.0f%%
                - Average survival time: %.0f seconds
                - Longest confirmed kill: %.0f meters

                Player profile (derived deterministically from season stats, not by you):
                - Archetype: %s
                - Radar scores (0-100 scale, higher is stronger): Combat %.0f, Survival %.0f, Precision %.0f, Aggression %.0f, Support %.0f, Consistency %.0f
                """.formatted(
                match.mapName(), match.gameMode(), match.winPlace(), match.kills(),
                match.headshotRate() * 100, match.damageDealt(), match.timeSurvivedSeconds(),
                damageDelta, survivalDelta, headshotRateDelta,
                seasonStats.winRate() * 100, seasonStats.wins(), seasonStats.roundsPlayed(),
                seasonStats.avgDamage(), seasonStats.killDeathRatio(), seasonStats.headshotRate() * 100,
                seasonStats.top10Rate() * 100, seasonStats.avgSurvivalSeconds(), seasonStats.longestKillMeters(),
                seasonStats.archetype(),
                radar.combat(), radar.survival(), radar.precision(), radar.aggression(), radar.support(), radar.consistency());
    }

    // Guards against a zero season average (e.g. a brand-new player) rather than dividing by zero.
    private String describeDeltaVsSeasonAverage(double matchValue, double seasonAverage) {
        if (seasonAverage == 0) {
            return "no season average available yet";
        }
        double percentChange = ((matchValue - seasonAverage) / seasonAverage) * 100;
        return "%+.0f%% vs season average".formatted(percentChange);
    }

    private InsightDto parseInsight(String rawText) {
        String summary = extractSection(rawText, "SUMMARY");
        if (summary.isEmpty()) {
            // Gemini didn't follow the requested format; fall back to the raw text as the
            // summary rather than failing the request, still tagged as a genuine "gemini" source.
            return new InsightDto(rawText.trim(), List.of(), List.of(), List.of(),
                    "", "", List.of(), List.of(), GEMINI_SOURCE);
        }

        return new InsightDto(
                summary,
                extractList(rawText, "STRENGTHS"),
                extractList(rawText, "WEAKNESSES"),
                extractList(rawText, "RECOMMENDATIONS"),
                extractSection(rawText, "PLAYSTYLE"),
                extractSection(rawText, "SEASON_PROGRESS"),
                extractList(rawText, "RISK_FACTORS"),
                extractList(rawText, "TRAINING_PRIORITIES"),
                GEMINI_SOURCE);
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
