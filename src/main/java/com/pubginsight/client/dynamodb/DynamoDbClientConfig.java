package com.pubginsight.client.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

// Produces the DynamoDbEnhancedClient bean AnalysisHistoryRepository builds its table
// reference from. Region is read via @Value("${aws.region}") rather than a dedicated
// properties record since it's a single scalar shared across future AWS services (S3),
// not something specific to DynamoDB - keeps AwsDynamoDbProperties scoped to the
// dynamodb.* keys only.
//
// No explicit credentials provider is configured: on the Learner Lab / EC2-backed
// Elastic Beanstalk environment (see docs/deliverables/ARCHITECTURE.md D8), the SDK's default
// credentials provider chain resolves the LabRole's credentials automatically. Building
// the client here does not make a network call, so this bean is safe to construct even
// when no AWS credentials are present (e.g. in tests) - failures only surface when a
// real DynamoDB call is made, which is why AnalysisHistoryRepository wraps its calls in
// try/catch rather than relying on bean construction to fail fast.
@Configuration
public class DynamoDbClientConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .build();

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
