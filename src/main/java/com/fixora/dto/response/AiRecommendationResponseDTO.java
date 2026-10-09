package com.fixora.dto.response;

import java.util.List;

/**
 * Recommendations.
 *
 * `mode` is always "rules" for the current implementation: scoring is a
 * transparent rule-based algorithm over real catalogue data, optionally phrased
 * by the AI. The API says so honestly instead of claiming AI generation.
 */
public record AiRecommendationResponseDTO(
        String mode,
        String explanation,
        List<RecommendedService> recommendations
) {
    public record RecommendedService(
            ServiceResponseDTO service,
            int score,
            String reason
    ) {
    }
}
