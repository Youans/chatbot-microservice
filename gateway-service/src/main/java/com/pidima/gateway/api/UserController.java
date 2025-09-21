package com.pidima.gateway.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/me")
public class UserController {

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "name", auth.getName(),
                "authorities", auth.getAuthorities(),
                "details", auth.getDetails()
        );
    }
}
