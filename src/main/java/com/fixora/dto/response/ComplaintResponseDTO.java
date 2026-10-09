package com.fixora.dto.response;

import com.fixora.enums.ComplaintCategory;
import com.fixora.enums.ComplaintPriority;
import com.fixora.enums.ComplaintStatus;

import java.time.Instant;

public record ComplaintResponseDTO(
        Long id,
        String subject,
        String description,
        ComplaintCategory category,
        ComplaintStatus status,
        ComplaintPriority priority,
        Long bookingId,
        String bookingRef,
        Long customerId,
        String customerName,
        String aiSuggestedCategory,
        String aiSuggestedSummary,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt
) {
}
