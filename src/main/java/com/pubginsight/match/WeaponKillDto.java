package com.pubginsight.match;

// Public response shape for GET /api/players/{playerId}/matches/{matchId}/weapons - a
// telemetry-derived breakdown of one player's kills in one match, by weapon. Deliberately a
// brand new DTO/endpoint rather than an addition to MatchDto - see WeaponBreakdownController.
public record WeaponKillDto(String weapon, int kills) {
}
