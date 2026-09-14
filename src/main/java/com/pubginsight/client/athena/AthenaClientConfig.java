package com.pubginsight.client.athena;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;

// Same pattern as S3ClientConfig/DynamoDbClientConfig: building the client makes no network
// call, so this bean is safe to construct even without AWS credentials present (e.g. tests).
@Configuration
public class AthenaClientConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public AthenaClient athenaClient() {
        return AthenaClient.builder()
                .region(Region.of(region))
                .build();
    }
}
