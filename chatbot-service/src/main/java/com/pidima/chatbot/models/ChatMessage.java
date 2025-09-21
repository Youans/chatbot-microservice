package com.pidima.chatbot.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role; // user|assistant|system
    private String content;
    private Instant timestamp = Instant.now();
}
