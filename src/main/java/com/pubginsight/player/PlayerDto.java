package com.pubginsight.player;

import java.util.List;

public record PlayerDto(String id, String name, String shardId, List<String> recentMatchIds) {
}
