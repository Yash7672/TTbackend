package com.fixora.dto.response;

import com.fixora.enums.PricingType;

import java.math.BigDecimal;

public record ServicePackageResponseDTO(
        Long id,
        Long serviceId,
        String serviceName,
        String name,
        String description,
        BigDecimal price,
        Integer durationMinutes,
        String includedWork,
        String excludedWork,
        PricingType pricingType,
        Boolean active,
        Integer displayOrder
) {
}
