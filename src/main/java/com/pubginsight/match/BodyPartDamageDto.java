package com.pubginsight.match;

// Hit counts per body-part location for this player's landed attacks in a match.
// Sourced from telemetry's damageReason enum, not inferred from weapon type.
public record BodyPartDamageDto(String label, int hits) {
}
