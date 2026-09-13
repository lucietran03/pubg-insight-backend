package com.pubginsight.client.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

// No explicit credentials provider: the SDK's default chain resolves credentials from the
// environment. Building the client makes no network call, so this bean is safe to construct
// even without AWS credentials present (e.g. in tests) - failures surface only on a real call.
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
