package com.fixora.dto.response;

/** Honest report of the AI configuration for the admin AI status page. */
public record AiStatusDTO(
        String configuredProvider,
        String activeMode,
        String model,
        boolean apiKeyConfigured,
        boolean fallbackAvailable,
        long catalogueServices,
        long catalogueCategories,
        String note
) {
}
