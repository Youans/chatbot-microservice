package com.pidima.chatbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Hard override CORS filter for local testing.
 * Always sets permissive CORS headers and short-circuits OPTIONS requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsOverrideFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Always add permissive headers
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            origin = "*";
        }
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Expose-Headers", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        // Allow credentials when a specific Origin is present (browser requires non-wildcard)
        response.setHeader("Access-Control-Allow-Credentials", origin.equals("*") ? "false" : "true");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
