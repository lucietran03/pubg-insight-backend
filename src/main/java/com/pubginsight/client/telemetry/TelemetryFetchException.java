package com.pubginsight.client.telemetry;

// Unlike PubgApiException/GeminiApiException, this is always caught and soft-failed by the
// caller - a telemetry fetch failure must never turn into a hard error for the match page.
public class TelemetryFetchException extends RuntimeException {

    public TelemetryFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
