package com.fixora.dto.response;

import java.util.List;

/** AI-written service copy. Always a DRAFT: an admin must review before publishing. */
public record GeneratedDescriptionResponseDTO(
        String shortDescription,
        String fullDescription,
        List<String> faqSuggestions,
        String packageExplanation,
        boolean draft,
        String mode
) {
}
