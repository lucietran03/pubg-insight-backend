package com.pubginsight.client.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// Produces the S3Client bean S3MatchCacheClient builds on. Region is read via
// @Value("${aws.region}") rather than a dedicated properties record, matching
// DynamoDbClientConfig's convention: it's a single scalar shared across every AWS service
// this app uses, not something specific to S3, so it stays out of AwsS3Properties (which
// is scoped to the s3.* keys only).
//
// No explicit credentials provider is configured: on the Learner Lab / EC2-backed Elastic
// Beanstalk environment (see docs/deliverables/ARCHITECTURE.md D8), the SDK's default credentials
// provider chain resolves the LabRole's credentials automatically. Building the client here
// does not make a network call, so this bean is safe to construct even when no AWS
// credentials are present (e.g. in tests) - failures only surface when a real S3 call is
// made, which is why S3MatchCacheClient wraps its calls in try/catch rather than relying on
// bean construction to fail fast.
@Configuration
public class S3ClientConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
