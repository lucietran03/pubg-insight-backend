package com.pubginsight.insight;

import com.pubginsight.client.gemini.GeminiApiClient;
import com.pubginsight.client.gemini.GeminiApiException;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import com.pubginsight.player.PlayerService;
import com.pubginsight.player.RadarScores;
import com.pubginsight.player.SeasonStatsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private MatchService matchService;

    @Mock
    private GeminiApiClient geminiApiClient;

    @Mock
    private OfflineInsightWriter offlineInsightWriter;

    @InjectMocks
    private InsightService insightService;

    // These tests only care about the insight-generation flow, not the analytics values
    // themselves (see PlayerMapperTest for those) - fill the new fields with plausible
    // placeholders rather than repeating them at every call site.
    private static SeasonStatsDto seasonStats(int wins, int roundsPlayed, double winRate) {
        return new SeasonStatsDto(
                wins, roundsPlayed, winRate,
                300.0, 1.5, 0.3, 0.4, 600.0, 150.0,
                new RadarScores(50, 50, 50, 50, 50, 50),
                "Balanced Operator"
        );
    }

    @Test
    void parsesWellFormattedGeminiResponse() {
        when(playerService.getSeasonStats("account.1"))
                .thenReturn(seasonStats(29, 265, 0.109));
        when(matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .thenReturn(new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1, "2026-09-06T00:00:00Z"));
        when(geminiApiClient.generateText(anyString())).thenReturn("""
                SUMMARY: Solid aggressive performance with a strong finish.
                STRENGTHS: high headshot rate, good damage output
                WEAKNESSES: low survival time
                RECOMMENDATIONS: rotate earlier, play more passively in the late game
                """);

        InsightDto result = insightService.generateInsights("account.1", "match-1");

        assertThat(result.summary()).contains("Solid aggressive performance");
        assertThat(result.strengths()).containsExactly("high headshot rate", "good damage output");
        assertThat(result.weaknesses()).containsExactly("low survival time");
        assertThat(result.recommendations()).containsExactly("rotate earlier", "play more passively in the late game");
        assertThat(result.source()).isEqualTo("gemini");
    }

    @Test
    void fallsBackToRawTextWhenFormatNotFollowed() {
        when(playerService.getSeasonStats("account.1"))
                .thenReturn(seasonStats(0, 0, 0.0));
        when(matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .thenReturn(new MatchDto("match-1", "Erangel", "squad", 0, 0, 0.0, 0.0, 0.0, 50, "2026-09-06T00:00:00Z"));
        when(geminiApiClient.generateText(anyString())).thenReturn("Just a plain sentence with no labels.");

        InsightDto result = insightService.generateInsights("account.1", "match-1");

        assertThat(result.summary()).isEqualTo("Just a plain sentence with no labels.");
        assertThat(result.strengths()).isEmpty();
        assertThat(result.weaknesses()).isEmpty();
        assertThat(result.recommendations()).isEmpty();
        assertThat(result.source()).isEqualTo("gemini");
    }

    @Test
    void cachesInsightSoRepeatedViewsOfSameMatchSkipGemini() {
        when(playerService.getSeasonStats("account.1"))
                .thenReturn(seasonStats(29, 265, 0.109));
        when(matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .thenReturn(new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1, "2026-09-06T00:00:00Z"));
        when(geminiApiClient.generateText(anyString())).thenReturn("""
                SUMMARY: Solid aggressive performance with a strong finish.
                STRENGTHS: high headshot rate
                WEAKNESSES: low survival time
                RECOMMENDATIONS: rotate earlier
                """);

        InsightDto first = insightService.generateInsights("account.1", "match-1");
        InsightDto second = insightService.generateInsights("account.1", "match-1");

        assertThat(second).isEqualTo(first);
        verify(geminiApiClient, times(1)).generateText(anyString());
        verify(playerService, times(1)).getSeasonStats("account.1");
        verify(matchService, times(1)).getMatchStatsForPlayer("match-1", "account.1");
    }

    @Test
    void fallsBackToOfflineInsightWhenGeminiIsUnavailableAndDoesNotCacheIt() {
        SeasonStatsDto stats = seasonStats(0, 0, 0.0);
        MatchDto match = new MatchDto("match-2", "Erangel", "squad", 0, 0, 0.0, 100.0, 200.0, 50, "2026-09-06T00:00:00Z");
        InsightDto offlineInsight = new InsightDto("Offline summary.", List.of(), List.of("short survival time"), List.of(), "offline");

        when(playerService.getSeasonStats("account.1")).thenReturn(stats);
        when(matchService.getMatchStatsForPlayer("match-2", "account.1")).thenReturn(match);
        when(geminiApiClient.generateText(anyString())).thenThrow(new GeminiApiException("Gemini exhausted", null));
        when(offlineInsightWriter.write(match, stats)).thenReturn(offlineInsight);

        InsightDto first = insightService.generateInsights("account.1", "match-2");
        InsightDto second = insightService.generateInsights("account.1", "match-2");

        assertThat(first.source()).isEqualTo("offline");
        assertThat(second.source()).isEqualTo("offline");
        // Not cached: each call retries Gemini in case it has recovered.
        verify(geminiApiClient, times(2)).generateText(anyString());
    }
}
