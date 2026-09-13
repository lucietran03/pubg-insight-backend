package com.pubginsight.client.dynamodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

// Partition key = playerId, sort key = matchId: a player has at most one stored analysis
// per match, so the pair is a natural unique key.
@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHistoryItem {

    private String playerId;
    private String matchId;
    private String mapName;
    private String gameMode;
    private Integer kills;
    private Double headshotRate;
    private Double damageDealt;
    private Double timeSurvivedSeconds;
    private Integer winPlace;
    private String insightSummary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private String createdAt;

    // Enhanced Client requires key annotations on the getter, not the field.
    @DynamoDbPartitionKey
    public String getPlayerId() {
        return playerId;
    }

    @DynamoDbSortKey
    public String getMatchId() {
        return matchId;
    }
}
