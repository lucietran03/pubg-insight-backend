package com.pubginsight.match;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players/{playerId}/matches/{matchId}")
public class WeaponBreakdownController {

    private final WeaponBreakdownService weaponBreakdownService;

    public WeaponBreakdownController(WeaponBreakdownService weaponBreakdownService) {
        this.weaponBreakdownService = weaponBreakdownService;
    }

    @GetMapping("/weapons")
    public MatchCombatBreakdownDto getWeaponBreakdown(@PathVariable String playerId, @PathVariable String matchId) {
        return weaponBreakdownService.getWeaponBreakdown(matchId, playerId);
    }
}
