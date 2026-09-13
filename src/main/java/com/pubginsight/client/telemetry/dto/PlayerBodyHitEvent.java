package com.pubginsight.client.telemetry.dto;

// One LogPlayerTakeDamage telemetry event where the queried player is the ATTACKER (i.e. a
// hit this player landed on someone else), attributed by raw "damageReason" enum value
// (e.g. "HeadShot", "TorsoShot") exactly as PUBG's telemetry reports it - see
// enums/telemetry/damageReason.json in github.com/pubg/api-assets, cross-checked against the
// community pubgjava client's DamageReason enum (same values, same field name). Mapping this
// raw reason string to a human body-part label ("Head", "Torso", ...) happens in
// match.WeaponBreakdownService, same layering as PlayerKillEvent's weapon id being bucketed
// into WeaponKillDto there rather than in this client package.
public record PlayerBodyHitEvent(String damageReason) {
}
