package com.pubginsight.player;

import com.pubginsight.client.dynamodb.AwsDynamoDbProperties;
import com.pubginsight.client.dynamodb.SeasonStatsCacheException;
import com.pubginsight.client.dynamodb.SeasonStatsCacheItem;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Optional;

@Repository
public class SeasonStatsCacheRepository {

    private final DynamoDbTable<SeasonStatsCacheItem> table;

    public SeasonStatsCacheRepository(DynamoDbEnhancedClient enhancedClient, AwsDynamoDbProperties properties) {
        this.table = enhancedClient.table(properties.seasonStatsCacheTable(), TableSchema.fromBean(SeasonStatsCacheItem.class));
    }

    public void save(SeasonStatsCacheItem item) {
        try {
            table.putItem(item);
        } catch (DynamoDbException | SdkException e) {
            throw new SeasonStatsCacheException(
                    "Failed to write season-stats cache entry for player '" + item.getPlayerId() + "'", e);
        }
    }

    public Optional<SeasonStatsCacheItem> findByPlayerId(String playerId) {
        try {
            return Optional.ofNullable(table.getItem(Key.builder().partitionValue(playerId).build()));
        } catch (DynamoDbException | SdkException e) {
            throw new SeasonStatsCacheException(
                    "Failed to read season-stats cache entry for player '" + playerId + "'", e);
        }
    }
}
