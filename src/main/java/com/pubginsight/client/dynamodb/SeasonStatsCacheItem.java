package com.pubginsight.client.dynamodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

// json holds a serialized SeasonStatsDto rather than mapped fields, since season stats
// change shape often and this cache should never need a schema migration to keep up.
// expiresAt doubles as the table's DynamoDB TTL attribute for automatic expiry.
@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonStatsCacheItem {

    private String playerId;
    private String json;
    private Long expiresAt;

    @DynamoDbPartitionKey
    public String getPlayerId() {
        return playerId;
    }
}
