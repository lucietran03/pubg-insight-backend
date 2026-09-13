package com.pubginsight.client.telemetry.dto;

// One LogPlayerKill telemetry event attributed to the queried player. distanceMeters is null
// when the event genuinely has no distance field (rather than defaulting to 0, which would
// misleadingly look like a real point-blank kill) - PUBG telemetry reports raw distance in
// centimeters, already converted to meters here so callers never have to remember that unit
// quirk themselves.
public record PlayerKillEvent(String weaponId, String weaponName, Double distanceMeters) {
}
