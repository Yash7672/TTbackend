package com.fixora.dto.response;

import com.fixora.enums.ProviderStatus;
import com.fixora.enums.UserRole;

import java.time.Instant;

/** User view. The password field does not exist here and can never be returned. */
public record UserResponseDTO(
        Long id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        Boolean active,
        String city,
        Long providerId,
        ProviderStatus providerStatus,
        Instant createdAt
) {
}
