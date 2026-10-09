package com.fixora.dto.response;

import com.fixora.enums.ProviderStatus;

import java.math.BigDecimal;

public record ProviderResponseDTO(
        Long id,
        Long userId,
        String businessName,
        String bio,
        String city,
        String area,
        Integer experienceYears,
        ProviderStatus status,
        BigDecimal ratingAverage,
        Integer ratingCount,
        String profileImageUrl,
        String contactName,
        long serviceCount
) {
}
