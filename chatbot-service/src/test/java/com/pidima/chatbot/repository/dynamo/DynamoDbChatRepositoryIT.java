package com.pidima.chatbot.repository.dynamo;

import com.pidima.chatbot.models.ChatMessage;
import com.pidima.chatbot.repository.ChatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class DynamoDbChatRepositoryIT {

    @Container
    static GenericContainer<?> dynamodbLocal = new GenericContainer<>(DockerImageName.parse("amazon/dynamodb-local:1.22.0"))
            .withExposedPorts(8000)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(java.time.Duration.ofSeconds(60));

    @BeforeAll
    static void checkDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available for Testcontainers - skipping IT");
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        String endpoint = String.format("http://%s:%d", dynamodbLocal.getHost(), dynamodbLocal.getMappedPort(8000));
        registry.add("app.persistence", () -> "dynamodb");
        registry.add("app.dynamodb-endpoint", () -> endpoint);
        registry.add("AWS_REGION", () -> "eu-central-1");
        registry.add("AWS_ACCESS_KEY_ID", () -> "test");
        registry.add("AWS_SECRET_ACCESS_KEY", () -> "test");
        // LLM not used in this test; keep dummy
        registry.add("app.llm-provider", () -> "dummy");
        registry.add("app.llm-model", () -> "dummy-model");
    }

    @Autowired
    private ChatRepository repository;

    @Test
    void append_and_read_history_works() {
        String sessionId = "it-session-" + System.currentTimeMillis();
        // Initially, session does not exist (no items yet)
        Assertions.assertFalse(repository.sessionExists(sessionId));
        // Append first message creates the item
        repository.appendMessage(sessionId, new ChatMessage("user", "hello", Instant.now()));
        Assertions.assertTrue(repository.sessionExists(sessionId));
        List<ChatMessage> history = repository.getHistory(sessionId);
        Assertions.assertEquals(1, history.size());
        Assertions.assertEquals("user", history.get(0).getRole());
        Assertions.assertEquals("hello", history.get(0).getContent());
    }

    @Test
    void get_history_of_unknown_session_throws() {
        String missing = "missing-" + System.nanoTime();
        Assertions.assertThrows(NoSuchElementException.class, () -> repository.getHistory(missing));
    }
}
