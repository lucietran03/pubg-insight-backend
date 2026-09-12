package com.pubginsight.insight;

import java.util.List;

// source is "gemini" for a genuine AI-generated result or "offline" for the
// network-free fallback (see OfflineInsightWriter) - callers must not present the two
// interchangeably, so the field always travels with the content it describes.
//
// playstyle/seasonProgress/riskFactors/trainingPriorities are the V2 "AI coach" sections:
// like strengths/weaknesses/recommendations, any section Gemini doesn't return is left
// empty (never fabricated/defaulted to placeholder text) - see InsightService.parseInsight.
// OfflineInsightWriter has no Gemini call to draw from, so it always leaves these empty.
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
