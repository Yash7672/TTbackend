package com.fixora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Natural-language search, e.g. "I need deep cleaning for my apartment under 2000". */
public record AiSearchRequestDTO(

        @NotBlank(message = "query is required")
        @Size(max = 500, message = "Query must be at most 500 characters")
        String query,

        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @PositiveOrZero(message = "maxPrice cannot be negative")
        BigDecimal maxPrice
) {
}
