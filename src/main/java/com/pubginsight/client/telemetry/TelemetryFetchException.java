package com.pubginsight.client.telemetry;

// Deliberately NOT registered in common.exception.GlobalExceptionHandler - unlike
// PubgApiException/GeminiApiException, this is always caught and soft-failed inside
// match.WeaponBreakdownService (see the comment there): a telemetry file being missing,
// unparseable, or slow to fetch must never turn into a hard error for a brand-new,
// strictly-additive "Weapons Used" panel bolted onto an already-working match page.
public class TelemetryFetchException extends RuntimeException {

    public TelemetryFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
