package com.pidima.chatbot.services;

import com.pidima.chatbot.models.ChatMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.llm-provider", havingValue = "dummy", matchIfMissing = true)
public class DummyLlmClient implements LLMClient {
    @Override
    public String generateReply(List<ChatMessage> history) {
        // Very simple mock: echo the last user message and summarize count
        String lastUser = history.stream()
                .filter(m -> "user".equals(m.getRole()))
                .reduce((first, second) -> second)
                .map(ChatMessage::getContent)
                .orElse("Hello! How can I help you with your documentation today?");
        long turns = history.stream().filter(m -> "user".equals(m.getRole())).count();
        return "You said: '" + lastUser + "'. (turn " + turns + ")";
    }
}
