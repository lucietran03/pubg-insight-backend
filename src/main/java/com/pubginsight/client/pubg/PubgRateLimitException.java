package com.pubginsight.client.pubg;

// PUBG's own rate limit (10 req/min on the free tier), distinct from a generic
// upstream failure - the caller can retry, it's not that the service is broken.
public class PubgRateLimitException extends RuntimeException {

    private final Long retryAfterSeconds;

    public PubgRateLimitException(Long retryAfterSeconds) {
        super("PUBG API rate limit reached");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
