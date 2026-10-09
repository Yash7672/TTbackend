package com.fixora.dto.response;

import com.fixora.enums.PricingType;

import java.math.BigDecimal;

public record ServiceResponseDTO(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String imageUrl,
        BigDecimal basePrice,
        PricingType pricingType,
        Integer durationMinutes,
        Boolean active,
        Boolean featured,
        BigDecimal ratingAverage,
        Integer ratingCount,
        long packageCount
) {
}
