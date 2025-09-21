package com.pidima.chatbot.services;

import com.pidima.chatbot.models.ChatMessage;
import com.pidima.chatbot.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Default implementation of {@link IChatService} that persists messages via a {@link com.pidima.chatbot.repository.ChatRepository}
 * and generates assistant replies using an {@link LLMClient}.
 */
@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {
    private final ChatRepository repository;
    private final LLMClient llmClient;

    @Override
    public String createSession(Optional<String> userId) {
        String sessionId = UUID.randomUUID().toString();
        repository.createSession(sessionId);
        userId.ifPresent(id -> repository.appendMessage(sessionId,
                new ChatMessage("system", "Session created for user: " + id, Instant.now())));
        return sessionId;
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId) {
        if (!repository.sessionExists(sessionId)) {
            throw new NoSuchElementException("Session not found: " + sessionId);
        }
        return repository.getHistory(sessionId);
    }

    @Override
    public String addMessageAndReply(String sessionId, String userMessage) {
        if (!repository.sessionExists(sessionId)) {
            throw new NoSuchElementException("Session not found: " + sessionId);
        }
        ChatMessage user = new ChatMessage("user", userMessage, Instant.now());
        repository.appendMessage(sessionId, user);
        List<ChatMessage> messages = repository.getHistory(sessionId);
        String reply = llmClient.generateReply(messages);
        ChatMessage assistant = new ChatMessage("assistant", reply, Instant.now());
        repository.appendMessage(sessionId, assistant);
        return reply;
    }
}
