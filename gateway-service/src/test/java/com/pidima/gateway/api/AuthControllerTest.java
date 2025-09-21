package com.pidima.gateway.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void login_returnsAccessToken_andSetsRefreshCookie() {
        var result = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{" +
                        "\"username\":\"admin\",\"password\":\"admin\"" +
                        "}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", ".*refresh_token=.*HttpOnly.*")
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isNumber();
    }

    @Test
    void refresh_withValidCookie_returnsNewAccessToken_andRotatesCookie() {
        // First login to obtain the refresh cookie
        var entity = webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{" +
                        "\"username\":\"admin\",\"password\":\"admin\"" +
                        "}")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String setCookie = entity.getResponseHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=");

        webTestClient.post()
                .uri("/auth/refresh")
                .cookie("refresh_token", extractCookieValue(setCookie, "refresh_token"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", ".*refresh_token=.*HttpOnly.*")
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer");
    }

    private static String extractCookieValue(String setCookie, String name) {
        for (String part : setCookie.split(";")) {
            String p = part.trim();
            if (p.startsWith(name + "=")) {
                return p.substring((name + "=").length());
            }
        }
        return "";
    }
}
