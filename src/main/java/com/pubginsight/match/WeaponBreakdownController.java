package com.pubginsight.match;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// New, additive endpoint - does not touch MatchController or its existing
// GET /api/players/{playerId}/matches/{matchId} route/shape. A separate controller class
// mapping into an overlapping URL prefix is fine: Spring dispatches by full path, and
// "/api/players/{playerId}/matches/{matchId}/weapons" never collides with MatchController's
// "/api/players/{playerId}/matches/{matchId}".
@RestController
@RequestMapping("/api/players/{playerId}/matches/{matchId}")
public class WeaponBreakdownController {

    private final WeaponBreakdownService weaponBreakdownService;

    public WeaponBreakdownController(WeaponBreakdownService weaponBreakdownService) {
        this.weaponBreakdownService = weaponBreakdownService;
    }

    @GetMapping("/weapons")
    public List<WeaponKillDto> getWeaponBreakdown(@PathVariable String playerId, @PathVariable String matchId) {
        return weaponBreakdownService.getWeaponBreakdown(matchId, playerId);
    }
}
