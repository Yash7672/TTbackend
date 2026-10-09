package com.fixora.dto.request;

import com.fixora.enums.PricingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin input for the AI service-description draft generator. */
public record GenerateDescriptionRequestDTO(

        @NotBlank(message = "serviceName is required")
        @Size(max = 160, message = "Service name must be at most 160 characters")
        String serviceName,

        @Size(max = 120, message = "Category must be at most 120 characters")
        String categoryName,

        @Size(max = 500, message = "Package names must be at most 500 characters")
        String packageNames,

        @Size(max = 2000, message = "Included work must be at most 2000 characters")
        String includedWork,

        @Size(max = 2000, message = "Excluded work must be at most 2000 characters")
        String excludedWork,

        PricingType pricingType
) {
}
