package com.pidima.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimitConfig {

    /**
     * Resolve rate limit key using JWT subject (sub) if present; otherwise fall back to remote IP.
     * Bean name referenced in application.yml as "@jwtKeyResolver".
     */
    @Bean("jwtKeyResolver")
    public KeyResolver jwtKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof Jwt jwt) {
                        String sub = Optional.ofNullable(jwt.getClaimAsString("sub")).orElse("anonymous");
                        return Mono.just("sub:" + sub);
                    }
                    return Mono.just("");
                })
                .defaultIfEmpty("")
                .flatMap(key -> {
                    if (key == null || key.isBlank()) {
                        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                        if (ip == null || ip.isBlank()) {
                            ip = exchange.getRequest().getRemoteAddress() != null ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
                        }
                        return Mono.just("ip:" + ip);
                    }
                    return Mono.just(key);
                });
    }
}
