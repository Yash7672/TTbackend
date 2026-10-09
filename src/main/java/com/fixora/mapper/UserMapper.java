package com.fixora.mapper;

import com.fixora.dto.response.UserResponseDTO;
import com.fixora.entity.Provider;
import com.fixora.entity.User;
import org.springframework.stereotype.Component;

/** Maps a User entity to a password-free DTO. */
@Component
public class UserMapper {

    /**
     * Call from inside a read-only transaction: the customer profile and provider
     * association are lazy (open-in-view is disabled on purpose).
     */
    public UserResponseDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        String city = null;
        if (user.getCustomerProfile() != null) {
            city = user.getCustomerProfile().getDefaultCity();
        }

        Long providerId = null;
        var providerStatus = (com.fixora.enums.ProviderStatus) null;
        Provider provider = user.getProvider();
        if (provider != null) {
            providerId = provider.getId();
            providerStatus = provider.getStatus();
            if (city == null) {
                city = provider.getCity();
            }
        }

        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getActive(),
                city,
                providerId,
                providerStatus,
                user.getCreatedAt()
        );
    }
}
