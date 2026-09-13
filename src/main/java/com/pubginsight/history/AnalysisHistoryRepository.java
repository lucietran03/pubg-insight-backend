package com.pubginsight.history;

import com.pubginsight.client.dynamodb.AnalysisHistoryException;
import com.pubginsight.client.dynamodb.AnalysisHistoryItem;
import com.pubginsight.client.dynamodb.AwsDynamoDbProperties;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;

// The only class in this feature that knows it's talking to DynamoDB; wraps the SDK's
// exception types in AnalysisHistoryException so callers don't depend on software.amazon.awssdk.
@Repository
public class AnalysisHistoryRepository {

    private final DynamoDbTable<AnalysisHistoryItem> table;

    public AnalysisHistoryRepository(DynamoDbEnhancedClient enhancedClient, AwsDynamoDbProperties properties) {
        this.table = enhancedClient.table(properties.analysisHistoryTable(), TableSchema.fromBean(AnalysisHistoryItem.class));
    }

    public void save(AnalysisHistoryItem item) {
        try {
            table.putItem(item);
        } catch (DynamoDbException e) {
            throw new AnalysisHistoryException(
                    "Failed to save analysis history for player '" + item.getPlayerId()
                            + "', match '" + item.getMatchId() + "'", e);
        } catch (SdkException e) {
            // Broader fallback for non-service SDK failures (e.g. network issues).
            throw new AnalysisHistoryException(
                    "DynamoDB call failed while saving analysis history for player '" + item.getPlayerId() + "'", e);
        }
    }

    public List<AnalysisHistoryItem> findByPlayerId(String playerId) {
        try {
            return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(playerId).build()))
                    .items()
                    .stream()
                    .toList();
        } catch (DynamoDbException e) {
            throw new AnalysisHistoryException(
                    "Failed to query analysis history for player '" + playerId + "'", e);
        } catch (SdkException e) {
            throw new AnalysisHistoryException(
                    "DynamoDB call failed while querying analysis history for player '" + playerId + "'", e);
        }
    }
}
