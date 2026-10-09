package com.fixora.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update payload for a provider profile. */
public record ProviderRequestDTO(

        @NotBlank(message = "Business name is required")
        @Size(max = 150, message = "Business name must be at most 150 characters")
        String businessName,

        @Size(max = 2000, message = "Bio must be at most 2000 characters")
        String bio,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @Size(max = 120, message = "Area must be at most 120 characters")
        String area,

        @Min(value = 0, message = "Experience cannot be negative")
        Integer experienceYears,

        @Size(max = 400, message = "Image URL must be at most 400 characters")
        String profileImageUrl
) {
}
