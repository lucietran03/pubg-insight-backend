package com.pubginsight.insight;

import java.util.List;

// source is "gemini" for a genuine AI-generated result or "offline" for the
// network-free fallback (see OfflineInsightWriter) - callers must not present the two
// interchangeably, so the field always travels with the content it describes.
public record InsightDto(String summary, List<String> strengths, List<String> weaknesses, List<String> recommendations, String source) {
}
