package com.pubginsight.insight;

import com.pubginsight.client.gemini.GeminiApiClient;
import com.pubginsight.client.gemini.GeminiApiException;
import com.pubginsight.client.gemini.GeminiRateLimitException;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import com.pubginsight.player.PlayerService;
import com.pubginsight.player.SeasonStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Orchestrates Player + Match features to gather already-aggregated metrics, then asks
// Gemini to turn them into a natural-language summary. Gemini never sees raw match
// telemetry - only the numbers PlayerService/MatchService already computed.
@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);
    private static final String GEMINI_SOURCE = "gemini";

    private final PlayerService playerService;
    private final MatchService matchService;
    private final GeminiApiClient geminiApiClient;
    private final OfflineInsightWriter offlineInsightWriter;

    // A generated insight for a given (player, match) never changes - the underlying
    // match is immutable and season stats are only prompt context - so a plain in-memory
    // cache is enough to stop repeat views of the same match from re-billing the paid
    // Gemini API. Single Spring Boot instance for a course project, not a distributed
    // system, so this deliberately isn't DynamoDB/Redis; a restart resetting it is fine.
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
            // Every configured Gemini model is exhausted (see GeminiApiClient.generateText).
            // Fall back to a deterministic, network-free summary instead of a 5xx - but
            // never cache it here: unlike a genuine Gemini result, an offline fallback
            // should be retried on the next request in case Gemini has recovered by then.
            log.warn("Gemini unavailable for player '{}' match '{}' - returning offline fallback insight",
                    playerId, matchId, e);
            return offlineInsightWriter.write(match, seasonStats);
        }

        insightCache.put(cacheKey, insight);
        return insight;
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
            // text as the summary rather than failing the whole request. Still a genuine
            // Gemini response, so it keeps the "gemini" source.
            return new InsightDto(rawText.trim(), List.of(), List.of(), List.of(), GEMINI_SOURCE);
        }

        return new InsightDto(
                summary,
                extractList(rawText, "STRENGTHS"),
                extractList(rawText, "WEAKNESSES"),
                extractList(rawText, "RECOMMENDATIONS"),
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
