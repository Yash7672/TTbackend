package com.fixora.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds the fixora.ai.* properties (see application.properties / .env.example). */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fixora.ai")
public class AiProperties {

    /** "fallback" (default, no key needed) or "anthropic". */
    private String provider = "fallback";

    /** Optional model override, e.g. claude-sonnet-4-5. */
    private String model;

    /** HTTP timeout in seconds before the fallback engine takes over. */
    private int timeoutSeconds = 20;

    private Anthropic anthropic = new Anthropic();

    @Getter
    @Setter
    public static class Anthropic {
        /** Backend-only secret. Never sent to the browser. */
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com/v1/messages";
        private String version = "2023-06-01";
        private int maxTokens = 1024;

        public boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /** Resolves the model id, defaulting to a current documented Anthropic model. */
    public String resolvedModel() {
        if (model != null && !model.isBlank()) {
            return model;
        }
        return "claude-sonnet-4-5";
    }

    public boolean isAnthropicSelected() {
        return "anthropic".equalsIgnoreCase(provider);
    }
}
