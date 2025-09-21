package com.pidima.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProps {
    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "dev-secret"; // HS256 secret
        private String issuer = "http://pidima.local";
        private String audience = "chatbot";
    }
}
