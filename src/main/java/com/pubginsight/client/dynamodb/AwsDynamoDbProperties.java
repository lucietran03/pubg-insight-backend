package com.pubginsight.client.dynamodb;

import org.springframework.boot.context.properties.ConfigurationProperties;

// The AWS region is shared across services and read separately via @Value in DynamoDbClientConfig.
@ConfigurationProperties(prefix = "aws.dynamodb")
public record AwsDynamoDbProperties(String analysisHistoryTable) {
}
