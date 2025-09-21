package com.pidima.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void options_preflight_is_permitted() {
        webTestClient.options()
                .uri("/api/chat/session")
                .header("Origin", "http://localhost:18082")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void health_is_public() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void api_is_authenticated_without_token_returns_401() {
        webTestClient.post()
                .uri("/api/chat/session")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
