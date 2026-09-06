package com.pubginsight.client.dynamodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

// DynamoDB Enhanced Client bean backing the "Analysis History" feature (Feature 4).
// Partition key = playerId, sort key = matchId: a player has at most one stored analysis
// per match, so (playerId, matchId) is a natural, unique composite key - no separate
// generated id is needed. createdAt is a plain ISO-8601 String stamped by the caller
// (HistoryService) at save time, not generated here, so this bean stays a passive data
// holder with no hidden side effects on construction.
//
// NOTE: annotation package/class names below (software.amazon.awssdk.enhanced.dynamodb.*)
// are believed correct for AWS SDK v2's dynamodb-enhanced module, but this could not be
// compiled against the real dependency in this sandbox (no network to resolve Maven
// artifacts). Verify with `mvn compile` once network/credentials are available.
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

    // Explicit getter overrides (Lombok skips generating these since they're already
    // declared) so the DynamoDB key annotations have somewhere to live - the Enhanced
    // Client requires key attributes to be marked on the getter, not the field.
    @DynamoDbPartitionKey
    public String getPlayerId() {
        return playerId;
    }

    @DynamoDbSortKey
    public String getMatchId() {
        return matchId;
    }
}
