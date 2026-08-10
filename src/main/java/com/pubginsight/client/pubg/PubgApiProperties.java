package com.pubginsight.client.pubg;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pubg.api")
public record PubgApiProperties(String baseUrl, String key, String defaultShard) {
}
