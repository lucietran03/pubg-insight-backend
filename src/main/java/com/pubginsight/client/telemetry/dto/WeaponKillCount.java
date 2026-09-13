package com.pubginsight.client.telemetry.dto;

// Internal result of tallying a single player's LogPlayerKill telemetry events by weapon.
// weaponId is the raw PUBG identifier (e.g. "WeapAK47_C"); weaponName is the resolved
// human-readable name from WeaponNameResolver (falls back to weaponId itself when the
// bundled dictionary doesn't recognize it - see WeaponNameResolver for why). Kept internal
// to client.telemetry - match.WeaponKillDto is the public-facing shape returned by the API.
public record WeaponKillCount(String weaponId, String weaponName, int kills) {
}
