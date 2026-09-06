package com.pubginsight.client.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {
}
