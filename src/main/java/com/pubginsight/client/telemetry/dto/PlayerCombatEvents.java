package com.pubginsight.client.telemetry.dto;

import java.util.List;

public record PlayerCombatEvents(List<PlayerKillEvent> kills, List<PlayerBodyHitEvent> bodyHits) {
}
