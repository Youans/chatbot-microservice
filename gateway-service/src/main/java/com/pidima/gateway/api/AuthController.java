package com.pidima.gateway.api;

import com.pidima.gateway.config.SecurityProps;
import com.pidima.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SecurityProps props;
    private final JwtService jwtService;

    private static final long ACCESS_TTL_SECONDS = 15 * 60; // 15 minutes
    private static final long REFRESH_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days
    private static final String REFRESH_COOKIE = "refresh_token";

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        // DEV ONLY: static credentials admin/admin
        if (!("admin".equals(username) && "admin".equals(password))) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        String sub = "admin";
        String accessToken = jwtService.generateAccessToken(sub, Map.of("roles", java.util.List.of("USER")), ACCESS_TTL_SECONDS);
        String refreshToken = jwtService.generateRefreshToken(sub, REFRESH_TTL_SECONDS);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(false) // set true in production with HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TTL_SECONDS)
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(Map.of(
                        "accessToken", accessToken,
                        "tokenType", "Bearer",
                        "expiresIn", ACCESS_TTL_SECONDS,
                        "sub", sub
                ));
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "missing_refresh_token"));
        }
        try {
            var claims = jwtService.parse(refreshToken);
            String sub = claims.getSubject();
            String accessToken = jwtService.generateAccessToken(sub, Map.of("roles", java.util.List.of("USER")), ACCESS_TTL_SECONDS);
            // rotate refresh token
            String newRefresh = jwtService.generateRefreshToken(sub, REFRESH_TTL_SECONDS);
            ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, newRefresh)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(REFRESH_TTL_SECONDS)
                    .build();
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(Map.of(
                            "accessToken", accessToken,
                            "tokenType", "Bearer",
                            "expiresIn", ACCESS_TTL_SECONDS,
                            "sub", sub
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_refresh_token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie clear = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header("Set-Cookie", clear.toString())
                .body(Map.of("ok", true));
    }
}
