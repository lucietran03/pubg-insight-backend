package com.pubginsight.client.telemetry.dto;

// distanceMeters is null (not 0) when the event has no distance field, and is already
// converted from PUBG's raw centimeters.
public record PlayerKillEvent(String weaponId, String weaponName, Double distanceMeters) {
}
