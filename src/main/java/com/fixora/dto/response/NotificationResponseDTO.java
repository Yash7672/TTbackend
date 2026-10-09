package com.fixora.dto.response;

import com.fixora.enums.NotificationType;

import java.time.Instant;

public record NotificationResponseDTO(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Long relatedBookingId,
        Instant createdAt
) {
}
