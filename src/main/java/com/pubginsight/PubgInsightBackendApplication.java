package com.pubginsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PubgInsightBackendApplication {

	public static void main(String[] args) {
		// Must be set here, not application.yml - DevTools' restart classloader forks before
		// yml is read, and the fork otherwise breaks DynamoDB enhanced client casts on hot-restart.
		System.setProperty("spring.devtools.restart.enabled", "false");
		SpringApplication.run(PubgInsightBackendApplication.class, args);
	}

}
