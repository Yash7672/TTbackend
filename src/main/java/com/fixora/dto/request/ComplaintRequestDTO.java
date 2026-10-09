package com.fixora.dto.request;

import com.fixora.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A customer complaint. `category` is optional — the AI may suggest one instead. */
public record ComplaintRequestDTO(

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must be at most 200 characters")
        String subject,

        @NotBlank(message = "Description is required")
        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        /** Optional booking this complaint refers to (ownership is verified). */
        Long bookingId,

        ComplaintCategory category
) {
}
