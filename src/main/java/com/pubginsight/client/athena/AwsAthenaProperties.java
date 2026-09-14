package com.pubginsight.client.athena;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.athena")
public record AwsAthenaProperties(String database, String table, String outputLocation) {
}
