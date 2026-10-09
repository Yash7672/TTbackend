package com.fixora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * FIXORA — Smart Local Services Marketplace.
 *
 * Modular monolith: controllers -> services -> repositories -> MySQL.
 * AI runs backend-only with a deterministic fallback, so the app starts and works
 * with no external API key configured.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FixoraApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixoraApplication.class, args);
    }
}
