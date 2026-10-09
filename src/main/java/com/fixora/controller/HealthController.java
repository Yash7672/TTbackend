package com.fixora.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/health — used by the Docker healthcheck and by the frontend footer.
 * It also proves the database connection works, not just that the JVM is up.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "fixora-backend");
        body.put("timestamp", Instant.now().toString());

        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            body.put("database", one != null && one == 1 ? "UP" : "UNKNOWN");
        } catch (RuntimeException ex) {
            body.put("status", "DEGRADED");
            body.put("database", "DOWN");
        }
        return body;
    }
}
