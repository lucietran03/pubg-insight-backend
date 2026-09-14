package com.pubginsight.player;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/{name}")
    public PlayerDto getPlayerByName(@PathVariable String name) {
        return playerService.searchPlayerByName(name);
    }

    // Distinct route (not an overload of getPlayerByName) since PUBG account ids and
    // display names are both single path segments with no shared prefix to disambiguate on.
    @GetMapping("/by-id/{accountId}")
    public PlayerDto getPlayerById(@PathVariable String accountId) {
        return playerService.findPlayerById(accountId);
    }

    @GetMapping("/{playerId}/season-stats")
    public SeasonStatsDto getSeasonStats(@PathVariable String playerId) {
        return playerService.getSeasonStats(playerId);
    }
}
