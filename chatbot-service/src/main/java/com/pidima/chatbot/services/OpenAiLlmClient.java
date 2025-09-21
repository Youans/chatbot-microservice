package com.pidima.chatbot.services;

import com.pidima.chatbot.config.AppProperties;
import com.pidima.chatbot.models.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.llm-provider", havingValue = "openai")
@RequiredArgsConstructor
@Log4j2
public class OpenAiLlmClient implements LLMClient {

    private final WebClient openAiWebClient;
    private final AppProperties appProperties;

    @Override
    public String generateReply(List<ChatMessage> history) {
        // Map our history to OpenAI chat messages
        List<Map<String, String>> messages = history.stream()
                .map(m -> {
                    Map<String, String> mm = new HashMap<>();
                    mm.put("role", mapRole(m.getRole()));
                    mm.put("content", m.getContent());
                    return mm;
                }).collect(Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", appProperties.getLlmModel());
        payload.put("messages", messages);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 512);

        try {
            Map<?, ?> resp = openAiWebClient
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(ex -> {
                        log.error("OpenAI request failed: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (resp == null) {
                return "I'm having trouble reaching the LLM service right now.";
            }
            // Extract first choice message.content
            Object choicesObj = resp.get("choices");
            if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
                Object choice0 = choices.get(0);
                if (choice0 instanceof Map<?, ?> c0) {
                    Object msg = c0.get("message");
                    if (msg instanceof Map<?, ?> m) {
                        Object content = m.get("content");
                        if (content != null) return content.toString();
                    }
                }
            }
            return "I couldn't parse a response from the LLM.";
        } catch (Exception e) {
            log.error("OpenAI call failed: ", e);
            return "An error occurred while generating a response.";
        }
    }

    private String mapRole(String role) {
        if (role == null) return "user";
        return switch (role) {
            case "assistant" -> "assistant";
            case "system" -> "system";
            default -> "user";
        };
    }
}
