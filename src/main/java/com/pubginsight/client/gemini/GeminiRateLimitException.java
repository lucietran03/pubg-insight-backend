package com.pubginsight.client.gemini;

// Gemini's free tier has its own request-rate limit, distinct from a generic upstream
// failure - the caller can retry shortly, it's not that AI Insights is broken.
public class GeminiRateLimitException extends RuntimeException {

    private final Long retryAfterSeconds;

    public GeminiRateLimitException(Long retryAfterSeconds) {
        super("Gemini API rate limit reached");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
