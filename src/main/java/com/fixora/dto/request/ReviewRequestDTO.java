package com.fixora.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Reviews are only accepted for the customer's own COMPLETED booking. */
public record ReviewRequestDTO(

        @NotNull(message = "bookingId is required")
        Long bookingId,

        @NotNull(message = "rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        String comment
) {
}
