package com.pubginsight.insight;

import java.util.List;

public record InsightDto(String summary, List<String> strengths, List<String> weaknesses, List<String> recommendations) {
}
