package com.pubginsight.client.pubg;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

// PUBG's 10 req/min limit applies to the whole app (one shared API key), not per
// browser tab or per user - a frontend can only ever pace its own requests, so this is
// the one place that can actually guarantee the app-wide budget is respected regardless
// of how many concurrent searches/tabs/users are hitting the backend at once.
//
// acquire() blocks the calling thread until a slot is free rather than rejecting
// immediately: a slower response reads to the user as normal loading, whereas a 429
// reads as something being broken. Blocking a Tomcat request thread for a few seconds is
// an acceptable trade-off at this app's scale (a course project demo, not a
// high-concurrency production service).
@Component
public class PubgRateLimiter {

    // One below PUBG's actual 10/min limit, as a small safety margin against clock/
    // latency skew between this app's window and PUBG's own.
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
