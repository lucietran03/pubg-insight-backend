package com.pubginsight.client.dynamodb;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds aws.dynamodb.analysis-history-table (see application.yml). The AWS region lives
// at the top-level "aws.region" key instead - it's shared by every AWS service this app
// uses (S3 included, once that lands), so it's read directly via @Value in
// DynamoDbClientConfig rather than duplicated into a dynamodb-specific properties record.
@ConfigurationProperties(prefix = "aws.dynamodb")
public record AwsDynamoDbProperties(String analysisHistoryTable) {
}
