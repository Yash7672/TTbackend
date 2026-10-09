package com.fixora.dto.request;

import com.fixora.enums.PricingType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Admin create/update payload for a service package. */
public record PackageRequestDTO(

        @NotNull(message = "serviceId is required")
        Long serviceId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "Price cannot be negative")
        BigDecimal price,

        @NotNull(message = "durationMinutes is required")
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 720, message = "Duration must be at most 720 minutes")
        Integer durationMinutes,

        @Size(max = 2000, message = "Included work must be at most 2000 characters")
        String includedWork,

        @Size(max = 2000, message = "Excluded work must be at most 2000 characters")
        String excludedWork,

        @NotNull(message = "pricingType is required")
        PricingType pricingType,

        Boolean active,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder
) {
}
