package com.pubginsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PubgInsightBackendApplication {

	public static void main(String[] args) {
		// Must be set before SpringApplication.run - DevTools' restart classloader forks
		// before Spring reads application.yml, so disabling it there has no effect. The
		// fork otherwise leaves two Class objects for the same class name alive across a
		// hot-restart, which fails DynamoDB enhanced client casts mid-restart.
		System.setProperty("spring.devtools.restart.enabled", "false");
		SpringApplication.run(PubgInsightBackendApplication.class, args);
	}

}
