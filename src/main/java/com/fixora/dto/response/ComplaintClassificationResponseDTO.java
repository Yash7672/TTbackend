package com.fixora.dto.response;

import com.fixora.enums.ComplaintCategory;
import com.fixora.enums.ComplaintPriority;

/**
 * AI suggestion for a complaint. Advisory only — the backend never changes a
 * complaint status or suspends a provider based on this.
 */
public record ComplaintClassificationResponseDTO(
        ComplaintCategory suggestedCategory,
        ComplaintPriority suggestedPriority,
        String summary,
        String note,
        String mode
) {
}
