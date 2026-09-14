package com.pubginsight.client.pubg;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PubgRateLimiterTest {

    // Only verifies the "under budget" path stays non-blocking; asserting the blocking
    // path would require waiting out a real ~60s window.
    @Test
    void allowsCallsUpToTheLimitWithoutBlocking() {
        PubgRateLimiter rateLimiter = new PubgRateLimiter();

        long start = System.nanoTime();
        for (int i = 0; i < 9; i++) {
            rateLimiter.acquire();
        }
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsedMillis).isLessThan(500);
    }
}
