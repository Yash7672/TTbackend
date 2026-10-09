package com.fixora.dto.response;

/**
 * Local development login result.
 *
 * There is no token: Fixora has no Spring Security yet. The frontend keeps
 * `user.id` and sends it as the X-User-Id header. This is explicitly NOT a
 * secure authentication mechanism.
 */
public record AuthResponseDTO(
        UserResponseDTO user,
        String message,
        boolean secureAuthentication
) {
}
