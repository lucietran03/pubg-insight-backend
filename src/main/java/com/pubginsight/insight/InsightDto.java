package com.pubginsight.insight;

import java.util.List;

// source is "gemini" or "offline" (see OfflineInsightWriter); any section Gemini
// doesn't return is left empty rather than fabricated.
public record InsightDto(
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        String playstyle,
        String seasonProgress,
        List<String> riskFactors,
        List<String> trainingPriorities,
        String source) {
}
