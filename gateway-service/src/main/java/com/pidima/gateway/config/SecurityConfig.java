package com.pidima.gateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(SecurityProps.class)
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final SecurityProps props;

    @Bean
    public org.springframework.security.oauth2.jwt.ReactiveJwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(props.getJwt().getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            Object aud = token.getClaims().get("aud");
            if (aud instanceof String s && s.equals(props.getJwt().getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            if (aud instanceof java.util.Collection<?> coll && coll.contains(props.getJwt().getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required audience is missing", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }

    @Value("${security.permitAllApi:true}")
    private boolean permitAllApi;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        http.authorizeExchange(ex -> {
            ex.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            ex.pathMatchers("/health", "/actuator/**").permitAll();
            ex.pathMatchers("/auth/**").permitAll();
            ex.pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
            if (permitAllApi) {
                ex.pathMatchers("/api/**").permitAll();
            } else {
                ex.pathMatchers("/api/**").authenticated();
            }
            ex.anyExchange().permitAll();
        });
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private Converter<org.springframework.security.oauth2.jwt.Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("roles");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(gac);
        return new ReactiveJwtAuthenticationConverterAdapter(conv);
    }

    @Bean
    public org.springframework.web.server.WebFilter corsWebFilter() {
        // Echo Origin and allow credentials for browser compatibility
        return (exchange, chain) -> {
            String origin = exchange.getRequest().getHeaders().getFirst(org.springframework.http.HttpHeaders.ORIGIN);
            if (origin == null || origin.isBlank()) {
                origin = "*";
            }
            org.springframework.http.server.reactive.ServerHttpResponse res = exchange.getResponse();
            res.getHeaders().set("Access-Control-Allow-Origin", origin);
            res.getHeaders().set("Access-Control-Allow-Methods", "*");
            res.getHeaders().set("Access-Control-Allow-Headers", "*");
            res.getHeaders().set("Access-Control-Expose-Headers", "*");
            res.getHeaders().set("Access-Control-Max-Age", "3600");
            res.getHeaders().set("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
            res.getHeaders().set("Access-Control-Allow-Credentials", origin.equals("*") ? "false" : "true");
            if (org.springframework.http.HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                res.setStatusCode(org.springframework.http.HttpStatus.OK);
                return res.setComplete();
            }
            return chain.filter(exchange);
        };
    }

    // Remove permissive ad-hoc CORS WebFilter to rely on CorsWebFilter above
}
