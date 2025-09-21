package com.pidima.chatbot.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessageResponse {
    private String sessionId;
    private String reply;
}
