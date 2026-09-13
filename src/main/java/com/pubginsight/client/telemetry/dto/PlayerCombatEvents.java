package com.pubginsight.client.telemetry.dto;

import java.util.List;

// Both telemetry-derived event lists TelemetryClient extracts for one player in one match, in
// a single streamed pass over the (potentially tens-of-MB) telemetry file: their LogPlayerKill
// events (kills) and the LogPlayerTakeDamage events where they were the attacker (bodyHits).
// Kept as one container/one fetch method rather than two separate fetches so the telemetry
// array is only ever streamed once.
public record PlayerCombatEvents(List<PlayerKillEvent> kills, List<PlayerBodyHitEvent> bodyHits) {
}
