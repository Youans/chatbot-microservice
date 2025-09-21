package com.pidima.chatbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private List<String> corsOrigins;
    private String llmProvider; // openai|bedrock|dummy
    private String llmModel;
    private String persistence; // memory|dynamodb

    // OpenAI config
    private String openaiApiKey;
    private String openaiBaseUrl; // e.g., https://api.openai.com/v1
}
