package com.pidima.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver principalNameKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> principal.getName())
                .switchIfEmpty(Mono.fromSupplier(() -> clientIp(exchange.getRequest())));
    }

    private String clientIp(ServerHttpRequest request) {
        // Try X-Forwarded-For first (behind ALB/ELB/Proxy), else remote address
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        InetSocketAddress addr = request.getRemoteAddress();
        return addr != null && addr.getAddress() != null
                ? addr.getAddress().getHostAddress()
                : "anonymous";
    }
}
