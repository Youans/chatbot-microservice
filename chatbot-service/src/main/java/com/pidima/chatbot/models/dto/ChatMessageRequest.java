package com.pidima.chatbot.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {
    @NotBlank
    @Size(min = 1, max = 64)
    private String sessionId;

    @NotNull
    @Size(min = 1, max = 5000)
    private String message;
}
