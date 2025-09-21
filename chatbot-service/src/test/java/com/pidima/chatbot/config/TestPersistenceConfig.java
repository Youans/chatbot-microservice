package com.pidima.chatbot.config;

import com.pidima.chatbot.models.ChatMessage;
import com.pidima.chatbot.repository.ChatRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class TestPersistenceConfig {

    @Bean
    @Primary
    public ChatRepository testInMemoryChatRepository() {
        return new ChatRepository() {
            private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();
            @Override
            public void createSession(String sessionId) {
                sessions.put(sessionId, Collections.synchronizedList(new ArrayList<>()));
            }
            @Override
            public boolean sessionExists(String sessionId) {
                return sessions.containsKey(sessionId);
            }
            @Override
            public void appendMessage(String sessionId, ChatMessage message) {
                List<ChatMessage> list = sessions.get(sessionId);
                if (list == null) throw new NoSuchElementException("Session not found: " + sessionId);
                list.add(message);
            }
            @Override
            public List<ChatMessage> getHistory(String sessionId) {
                List<ChatMessage> list = sessions.get(sessionId);
                if (list == null) throw new NoSuchElementException("Session not found: " + sessionId);
                return new ArrayList<>(list);
            }
        };
    }
}
