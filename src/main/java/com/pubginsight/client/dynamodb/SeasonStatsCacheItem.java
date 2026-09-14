package com.pubginsight.client.dynamodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonStatsCacheItem {

    private String playerId;
    // Serialized SeasonStatsDto, not mapped fields - season stats change shape too often
    // to keep migrating a fixed schema.
    private String json;
    // Doubles as the table's DynamoDB TTL attribute for automatic expiry.
    private Long expiresAt;

    @DynamoDbPartitionKey
    public String getPlayerId() {
        return playerId;
    }
}
