package com.pidima.gateway.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @GetMapping("/health")
    public ResponseEntity<HealthComponent> health() {
        // Proxy to actuator health endpoint (Spring Boot 3 returns HealthComponent)
        HealthComponent health = healthEndpoint.health();
        return ResponseEntity.ok(health);
    }
}
