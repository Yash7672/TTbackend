package com.fixora.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Input for the smart recommendation engine (rule-based scoring over real data). */
public record AiRecommendationRequestDTO(

        @Size(max = 500, message = "Query must be at most 500 characters")
        String query,

        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @PositiveOrZero(message = "maxPrice cannot be negative")
        BigDecimal maxPrice,

        @Min(value = 1, message = "limit must be at least 1")
        @Max(value = 24, message = "limit must be at most 24")
        Integer limit
) {
}
