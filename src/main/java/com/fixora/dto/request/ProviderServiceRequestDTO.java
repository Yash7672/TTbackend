package com.fixora.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Adds (or re-activates) one service in a provider's offering list. */
public record ProviderServiceRequestDTO(

        @NotNull(message = "serviceId is required")
        Long serviceId,

        @PositiveOrZero(message = "Custom price cannot be negative")
        BigDecimal customPrice,

        Boolean active
) {
}
