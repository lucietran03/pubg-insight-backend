package com.pubginsight.match;

import com.pubginsight.client.athena.AthenaAnalyticsClient;
import com.pubginsight.client.athena.PopulationComparison;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players/{playerId}/matches")
public class MatchController {

    private final MatchService matchService;
    private final AthenaAnalyticsClient athenaAnalyticsClient;

    public MatchController(MatchService matchService, AthenaAnalyticsClient athenaAnalyticsClient) {
        this.matchService = matchService;
        this.athenaAnalyticsClient = athenaAnalyticsClient;
    }

    @GetMapping("/{matchId}")
    public MatchDto getMatchStats(@PathVariable String playerId, @PathVariable String matchId) {
        return matchService.getMatchStatsForPlayer(matchId, playerId);
    }

    // Real population baseline from every match this app has ever analyzed for anyone,
    // queried live via Athena - not a fixed ceiling like the season radar scores.
    @GetMapping("/{matchId}/population-comparison")
    public PopulationComparison getPopulationComparison(@PathVariable String playerId, @PathVariable String matchId) {
        MatchDto match = matchService.getMatchStatsForPlayer(matchId, playerId);
        return athenaAnalyticsClient.compareDamage(match.gameMode(), match.damageDealt());
    }
}
