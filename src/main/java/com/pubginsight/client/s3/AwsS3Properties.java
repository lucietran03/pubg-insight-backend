package com.pubginsight.client.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds aws.s3.cache-bucket (see application.yml). The AWS region lives at the top-level
// "aws.region" key instead - it's shared by every AWS service this app uses (DynamoDB
// included), so it's read directly via @Value in S3ClientConfig rather than duplicated
// into an S3-specific properties record. Same convention as AwsDynamoDbProperties.
@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(String cacheBucket) {
}
