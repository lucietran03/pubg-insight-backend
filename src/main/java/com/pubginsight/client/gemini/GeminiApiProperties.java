package com.pubginsight.client.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Priority-ordered: GeminiApiClient tries models() first-to-last on failure, since
// rate limits/quotas are per-model, not per-application.
@ConfigurationProperties(prefix = "gemini.api")
public record GeminiApiProperties(String baseUrl, String key, List<String> models) {
}
