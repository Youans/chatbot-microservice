package com.pidima.chatbot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "app.llm-provider", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAIConfig {

    private final AppProperties appProperties;

    @Bean
    public WebClient openAiWebClient() {
        String baseUrl = appProperties.getOpenaiBaseUrl() != null ? appProperties.getOpenaiBaseUrl() : "https://api.openai.com/v1";
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenaiApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }
}
