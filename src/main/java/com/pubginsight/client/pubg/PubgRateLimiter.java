package com.pubginsight.client.pubg;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

// PUBG's 10 req/min limit is shared app-wide (one API key). acquire() blocks rather than
// rejecting, since a slow response reads as normal loading while a 429 reads as broken.
@Component
public class PubgRateLimiter {

    // One below PUBG's actual limit, as a safety margin against clock/latency skew.
    private static final int MAX_CALLS_PER_WINDOW = 9;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final Deque<Instant> callTimestamps = new ArrayDeque<>();

    public void acquire() {
        while (true) {
            long waitMillis;
            synchronized (callTimestamps) {
                Instant now = Instant.now();
                while (!callTimestamps.isEmpty() && Duration.between(callTimestamps.peekFirst(), now).compareTo(WINDOW) >= 0) {
                    callTimestamps.pollFirst();
                }
                if (callTimestamps.size() < MAX_CALLS_PER_WINDOW) {
                    callTimestamps.addLast(now);
                    return;
                }
                waitMillis = WINDOW.minus(Duration.between(callTimestamps.peekFirst(), now)).toMillis() + 50;
            }
            try {
                Thread.sleep(Math.max(waitMillis, 50));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PubgApiException("Interrupted while waiting for PUBG rate limit", e);
            }
        }
    }
}
