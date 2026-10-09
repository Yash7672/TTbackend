package com.fixora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable profile fields for the signed-in user. */
public record UpdateUserRequestDTO(

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        /** Preferred city, also stored on the customer profile when present. */
        @Size(max = 80, message = "City must be at most 80 characters")
        String city
) {
}
