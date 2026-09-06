package com.pubginsight.history;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players/{playerId}")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    // No request body: the match stats and AI insight are re-derived server-side from
    // playerId/matchId (via MatchService/InsightService), the same data the frontend
    // already fetched to display them - it shouldn't have to re-send it just to save it.
    @PostMapping("/matches/{matchId}/history")
    public AnalysisHistoryDto recordAnalysis(@PathVariable String playerId, @PathVariable String matchId) {
        return historyService.recordAnalysis(playerId, matchId);
    }

    @GetMapping("/history")
    public List<AnalysisHistoryDto> getHistory(@PathVariable String playerId) {
        return historyService.getHistoryForPlayer(playerId);
    }
}
