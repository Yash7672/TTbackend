package com.fixora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Input for AI complaint classification. The result is a suggestion for admins. */
public record ClassifyComplaintRequestDTO(

        @NotBlank(message = "subject is required")
        @Size(max = 200, message = "Subject must be at most 200 characters")
        String subject,

        @NotBlank(message = "description is required")
        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        @Size(max = 20, message = "Booking reference must be at most 20 characters")
        String bookingRef
) {
}
