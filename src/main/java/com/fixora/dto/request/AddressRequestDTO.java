package com.fixora.dto.request;

import com.fixora.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update payload for a customer address. */
public record AddressRequestDTO(

        @Size(max = 60, message = "Label must be at most 60 characters")
        String label,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 must be at most 200 characters")
        String line1,

        @Size(max = 200, message = "Address line 2 must be at most 200 characters")
        String line2,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @Size(max = 120, message = "Area must be at most 120 characters")
        String area,

        @Size(max = 12, message = "Pincode must be at most 12 characters")
        String pincode,

        AddressType addressType,

        Boolean defaultAddress
) {
}
