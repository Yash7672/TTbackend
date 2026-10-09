package com.fixora.dto.response;

import java.util.List;

/**
 * Assistant reply.
 *
 * `mode` is "fallback" when deterministic matching produced the answer and
 * "anthropic" when the external model did. `suggestedServices` always contains
 * real catalogue rows, never invented services.
 */
public record AiChatResponseDTO(
        String reply,
        String mode,
        String provider,
        String detectedCategory,
        List<String> suggestions,
        List<ServiceResponseDTO> suggestedServices,
        List<ServicePackageResponseDTO> highlightedPackages
) {
}
