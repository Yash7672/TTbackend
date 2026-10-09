package com.fixora.dto.request;

import com.fixora.enums.PricingType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Admin create/update payload for a service. */
public record ServiceRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 160, message = "Name must be at most 160 characters")
        String name,

        @NotNull(message = "categoryId is required")
        Long categoryId,

        @Size(max = 500, message = "Short description must be at most 500 characters")
        String shortDescription,

        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        @Size(max = 400, message = "Image URL must be at most 400 characters")
        String imageUrl,

        @NotNull(message = "basePrice is required")
        @PositiveOrZero(message = "Base price cannot be negative")
        BigDecimal basePrice,

        @NotNull(message = "pricingType is required")
        PricingType pricingType,

        @NotNull(message = "durationMinutes is required")
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 720, message = "Duration must be at most 720 minutes")
        Integer durationMinutes,

        Boolean active,

        Boolean featured
) {
}
