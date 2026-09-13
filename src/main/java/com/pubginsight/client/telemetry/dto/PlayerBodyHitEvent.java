package com.pubginsight.client.telemetry.dto;

// A hit this player landed on someone else, keyed by PUBG's raw damageReason value
// (e.g. "HeadShot", "TorsoShot").
public record PlayerBodyHitEvent(String damageReason) {
}
