package com.pidima.chatbot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "app.persistence", havingValue = "dynamodb")
public class DynamoConfig {
    @Value("${app.dynamodb-endpoint:}")
    private String endpointProp;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        String region = System.getenv().getOrDefault("AWS_REGION", "eu-central-1");
        String endpoint = System.getenv("DYNAMODB_ENDPOINT"); // optional (e.g., LocalStack)
        if ((endpoint == null || endpoint.isBlank()) && endpointProp != null && !endpointProp.isBlank()) {
            endpoint = endpointProp;
        }
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                .endpointOverride(URI.create(endpoint))
                // Use static dummy credentials for local DynamoDB to avoid env dependency
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }
}
