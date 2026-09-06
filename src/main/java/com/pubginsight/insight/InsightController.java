package com.pubginsight.insight;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players/{playerId}/matches/{matchId}/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping
    public InsightDto getInsights(@PathVariable String playerId, @PathVariable String matchId) {
        return insightService.generateInsights(playerId, matchId);
    }
}
