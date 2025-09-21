package com.pidima.chatbot.services;

import com.pidima.chatbot.models.ChatMessage;
import java.util.List;

public interface LLMClient {
    String generateReply(List<ChatMessage> history);
}
