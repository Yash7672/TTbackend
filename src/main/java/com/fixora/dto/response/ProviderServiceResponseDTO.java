package com.fixora.dto.response;

import java.math.BigDecimal;

public record ProviderServiceResponseDTO(
        Long id,
        Long providerId,
        Long serviceId,
        String serviceName,
        String categoryName,
        BigDecimal customPrice,
        BigDecimal cataloguePrice,
        Boolean active
) {
}
