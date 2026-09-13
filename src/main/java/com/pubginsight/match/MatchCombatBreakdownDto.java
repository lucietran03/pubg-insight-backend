package com.pubginsight.match;

import java.util.List;

// Response for GET /api/players/{playerId}/matches/{matchId}/weapons.
public record MatchCombatBreakdownDto(
        List<WeaponKillDto> weapons,
        List<DistanceBucketDto> shotDistances,
        List<BodyPartDamageDto> bodyPartDamage) {

    public static MatchCombatBreakdownDto empty() {
        return new MatchCombatBreakdownDto(List.of(), List.of(), List.of());
    }
}
