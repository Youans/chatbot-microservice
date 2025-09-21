package com.pidima.chatbot.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pidima.chatbot.models.dto.ChatMessageRequest;
import com.pidima.chatbot.models.dto.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@Import(com.pidima.chatbot.config.TestPersistenceConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createSessionAndSendMessageAndGetHistory() throws Exception {
        // Create session
        CreateSessionRequest csr = new CreateSessionRequest();
        csr.setUserId("user-123");
        MvcResult createResult = mockMvc.perform(post("/chat/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(csr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn();

        String sessionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("sessionId").asText();
        assertThat(sessionId).isNotBlank();

        // Send a message
        ChatMessageRequest cmr = new ChatMessageRequest();
        cmr.setSessionId(sessionId);
        cmr.setMessage("How do I deploy on AWS?");

        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.reply").exists());

        // Get history
        mockMvc.perform(get("/chat/history/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").exists());
    }

    @Test
    void validationShouldFailForEmptyMessage() throws Exception {
        ChatMessageRequest cmr = new ChatMessageRequest();
        cmr.setSessionId(" ");
        cmr.setMessage("");
        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
