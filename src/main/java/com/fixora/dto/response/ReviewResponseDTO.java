package com.fixora.dto.response;

import java.time.Instant;

public record ReviewResponseDTO(
        Long id,
        Long bookingId,
        Long serviceId,
        String serviceName,
        Long providerId,
        String providerName,
        Long customerId,
        String customerName,
        Integer rating,
        String comment,
        Boolean visible,
        Instant createdAt
) {
}
