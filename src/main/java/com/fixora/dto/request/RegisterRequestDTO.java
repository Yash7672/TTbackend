package com.fixora.dto.request;

import com.fixora.enums.UserRole;
import jakarta.validation.constraints.*;

/**
 * Registration payload. Providers may supply a business name and city; those
 * fields are ignored for customers.
 */
public record RegisterRequestDTO(

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 160, message = "Email must be at most 160 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 64, message = "Password must be between 6 and 64 characters")
        String password,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        /** CUSTOMER (default) or PROVIDER. ADMIN can never be self-registered. */
        UserRole role,

        @Size(max = 150, message = "Business name must be at most 150 characters")
        String businessName,

        @Size(max = 80, message = "City must be at most 80 characters")
        String city
) {
}
