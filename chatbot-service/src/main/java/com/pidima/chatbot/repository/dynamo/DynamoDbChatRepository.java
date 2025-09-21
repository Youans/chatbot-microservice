package com.pidima.chatbot.repository.dynamo;

import com.pidima.chatbot.models.ChatMessage;
import com.pidima.chatbot.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Repository
@ConditionalOnProperty(name = "app.persistence", havingValue = "dynamodb")
@RequiredArgsConstructor
public class DynamoDbChatRepository implements ChatRepository {
    private static final String TABLE_NAME = "chat_messages";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<ChatMessageItem> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ChatMessageItem.class));
    }

    @Override
    public void createSession(String sessionId) {
        // No-op for DynamoDB. Session existence is inferred by presence of messages keyed by sessionId.
    }

    @Override
    public boolean sessionExists(String sessionId) {
        PageIterable<ChatMessageItem> pages = table().query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(Key.builder().partitionValue(sessionId).build()))
                .limit(1));
        return pages.stream().findFirst().map(p -> p.items().size() > 0).orElse(false);
    }

    @Override
    public void appendMessage(String sessionId, ChatMessage message) {
        ChatMessageItem item = new ChatMessageItem();
        item.setSessionId(sessionId);
        // use epoch milli as sort key for ordering
        long ts = message.getTimestamp() != null ? message.getTimestamp().toEpochMilli() : Instant.now().toEpochMilli();
        item.setTs(ts);
        item.setRole(message.getRole());
        item.setContent(message.getContent());
        table().putItem(item);
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId) {
        PageIterable<ChatMessageItem> pages = table().query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(Key.builder().partitionValue(sessionId).build())));
        List<ChatMessage> out = new ArrayList<>();
        pages.stream().forEach(page -> page.items().forEach(i -> out.add(new ChatMessage(i.getRole(), i.getContent(),
                Instant.ofEpochMilli(i.getTs())))));
        if (out.isEmpty()) {
            throw new NoSuchElementException("Session not found: " + sessionId);
        }
        // DynamoDB returns in ascending order of sort key by default in enhanced client
        return out;
    }
}
