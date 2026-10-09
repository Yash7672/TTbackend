package com.fixora.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Local development login payload.
 *
 * WARNING: this project intentionally has no Spring Security / JWT yet, so there
 * is no real session or token. The frontend stores the returned user id and
 * sends it back as the X-User-Id header. That is NOT a security boundary.
 */
public record LoginRequestDTO(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
