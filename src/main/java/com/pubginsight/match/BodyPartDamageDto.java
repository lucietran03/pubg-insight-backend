package com.pubginsight.match;

// One body-part location and how many of the player's landed hits (as attacker) in this match
// struck it - e.g. ("Head", 7). Derived from telemetry's "damageReason" enum
// (see TelemetryClient), which only ever reports one of a small, fixed set of real hit
// locations - not fabricated from weapon type or any other guesswork.
public record BodyPartDamageDto(String label, int hits) {
}
