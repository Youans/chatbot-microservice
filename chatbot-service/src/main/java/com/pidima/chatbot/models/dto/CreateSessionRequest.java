package com.pidima.chatbot.models.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSessionRequest {
    @Size(max = 128)
    private String userId; // optional but useful for multi-tenant routing
}
