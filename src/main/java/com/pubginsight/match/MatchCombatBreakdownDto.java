package com.pubginsight.match;

import java.util.List;

// Public response shape for GET /api/players/{playerId}/matches/{matchId}/weapons - three
// telemetry-derived views over this player's combat activity in one match (weapon tally and
// shot-distance histogram from their kills, plus a body-part hit-location breakdown from
// every hit they landed as attacker), so the frontend gets all three from one call instead of
// several.
public record MatchCombatBreakdownDto(
        List<WeaponKillDto> weapons,
        List<DistanceBucketDto> shotDistances,
        List<BodyPartDamageDto> bodyPartDamage) {

    public static MatchCombatBreakdownDto empty() {
        return new MatchCombatBreakdownDto(List.of(), List.of(), List.of());
    }
}
