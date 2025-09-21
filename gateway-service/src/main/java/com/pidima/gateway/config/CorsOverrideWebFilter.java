package com.pidima.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Hard override CORS for local testing in the reactive gateway.
 * Always sets permissive CORS headers and short-circuits OPTIONS requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsOverrideWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String origin = request.getHeaders().getFirst("Origin");
        if (origin == null || origin.isBlank()) {
            origin = "*";
        }
        response.getHeaders().set("Access-Control-Allow-Origin", origin);
        response.getHeaders().set("Access-Control-Allow-Methods", "*");
        response.getHeaders().set("Access-Control-Allow-Headers", "*");
        response.getHeaders().set("Access-Control-Expose-Headers", "*");
        response.getHeaders().set("Access-Control-Max-Age", "3600");
        response.getHeaders().set("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        // Allow credentials only when a specific Origin is present
        response.getHeaders().set("Access-Control-Allow-Credentials", origin.equals("*") ? "false" : "true");

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }
        return chain.filter(exchange);
    }
}
