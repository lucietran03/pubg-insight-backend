package com.pubginsight.client.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

// The AWS region is shared across services and read separately via @Value in S3ClientConfig.
@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(String cacheBucket) {
}
