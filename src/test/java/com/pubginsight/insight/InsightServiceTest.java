package com.pubginsight.insight;

import com.pubginsight.client.gemini.GeminiApiClient;
import com.pubginsight.match.MatchDto;
import com.pubginsight.match.MatchService;
import com.pubginsight.player.PlayerService;
import com.pubginsight.player.SeasonStatsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private MatchService matchService;

    @Mock
    private GeminiApiClient geminiApiClient;

    @InjectMocks
    private InsightService insightService;

    @Test
    void parsesWellFormattedGeminiResponse() {
        when(playerService.getSeasonStats("account.1"))
                .thenReturn(new SeasonStatsDto(29, 265, 0.109));
        when(matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .thenReturn(new MatchDto("match-1", "Erangel", "squad", 4, 2, 0.5, 520.0, 1200.0, 1));
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
    }

    @Test
    void fallsBackToRawTextWhenFormatNotFollowed() {
        when(playerService.getSeasonStats("account.1"))
                .thenReturn(new SeasonStatsDto(0, 0, 0.0));
        when(matchService.getMatchStatsForPlayer("match-1", "account.1"))
                .thenReturn(new MatchDto("match-1", "Erangel", "squad", 0, 0, 0.0, 0.0, 0.0, 50));
        when(geminiApiClient.generateText(anyString())).thenReturn("Just a plain sentence with no labels.");

        InsightDto result = insightService.generateInsights("account.1", "match-1");

        assertThat(result.summary()).isEqualTo("Just a plain sentence with no labels.");
        assertThat(result.strengths()).isEmpty();
        assertThat(result.weaknesses()).isEmpty();
        assertThat(result.recommendations()).isEmpty();
    }
}
