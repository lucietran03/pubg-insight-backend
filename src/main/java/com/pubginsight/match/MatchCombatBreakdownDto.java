package com.pubginsight.match;

import java.util.List;

// Public response shape for GET /api/players/{playerId}/matches/{matchId}/weapons - both
// telemetry-derived views over the same set of kill events (weapon tally and shot-distance
// histogram), so the frontend gets both from one call instead of two.
public record MatchCombatBreakdownDto(List<WeaponKillDto> weapons, List<DistanceBucketDto> shotDistances) {

    public static MatchCombatBreakdownDto empty() {
        return new MatchCombatBreakdownDto(List.of(), List.of());
    }
}
