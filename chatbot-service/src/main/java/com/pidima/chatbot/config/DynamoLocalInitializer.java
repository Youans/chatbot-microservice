package com.pidima.chatbot.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import com.pidima.chatbot.repository.dynamo.ChatMessageItem;

/**
 * When APP_PERSISTENCE=dynamodb and a local endpoint is provided via DYNAMODB_ENDPOINT,
 * ensure the chat_messages table exists in the local DynamoDB instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.persistence", havingValue = "dynamodb")
public class DynamoLocalInitializer {
    private static final String TABLE_NAME = "chat_messages";

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient enhancedClient;

    @Value("${app.dynamodb-endpoint:}")
    private String endpointProp;

    @PostConstruct
    public void init() {
        String endpoint = System.getenv("DYNAMODB_ENDPOINT");
        if ((endpoint == null || endpoint.isBlank()) && endpointProp != null && !endpointProp.isBlank()) {
            endpoint = endpointProp;
        }
        if (endpoint == null || endpoint.isBlank()) {
            // Not running against local DynamoDB; skip auto-creation.
            return;
        }
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
            log.info("DynamoDB table '{}' already exists.", TABLE_NAME);
        } catch (ResourceNotFoundException rnfe) {
            log.info("DynamoDB table '{}' not found. Creating it in local DynamoDB...", TABLE_NAME);
            DynamoDbTable<ChatMessageItem> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ChatMessageItem.class));
            table.createTable(r -> r.provisionedThroughput(t -> t.readCapacityUnits(5L).writeCapacityUnits(5L)));
            log.info("DynamoDB table '{}' created.", TABLE_NAME);
        } catch (Exception e) {
            log.warn("Failed to ensure DynamoDB table exists: {}", e.getMessage());
        }
    }
}
