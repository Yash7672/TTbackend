package com.fixora.service.impl;

import com.fixora.dto.request.LoginRequestDTO;
import com.fixora.dto.request.RegisterRequestDTO;
import com.fixora.dto.response.AuthResponseDTO;
import com.fixora.entity.CustomerProfile;
import com.fixora.entity.Provider;
import com.fixora.entity.User;
import com.fixora.enums.ProviderStatus;
import com.fixora.enums.UserRole;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.DuplicateResourceException;
import com.fixora.exception.InvalidCredentialsException;
import com.fixora.mapper.UserMapper;
import com.fixora.repository.CustomerProfileRepository;
import com.fixora.repository.ProviderRepository;
import com.fixora.repository.UserRepository;
import com.fixora.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Local development authentication.
 *
 * There is no Spring Security, no JWT and no password hashing in this first
 * version — see the security limitations section of the README. Passwords are
 * never returned by any endpoint and never written to the logs.
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderRepository providerRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with " + email + " already exists.");
        }

        UserRole role = request.role() == null ? UserRole.CUSTOMER : request.role();
        if (role == UserRole.ADMIN) {
            throw new BadRequestException("Administrator accounts cannot be self-registered.");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(request.password())   // plain text: DEV ONLY
                .phone(request.phone())
                .role(role)
                .active(true)
                .build();
        user = userRepository.save(user);

        if (role == UserRole.PROVIDER) {
            Provider provider = Provider.builder()
                    .user(user)
                    .businessName(request.businessName() == null || request.businessName().isBlank()
                            ? request.fullName() + " Services"
                            : request.businessName().trim())
                    .city(request.city())
                    .status(ProviderStatus.PENDING)      // admins approve before listing
                    .ratingAverage(BigDecimal.ZERO)
                    .ratingCount(0)
                    .build();
            provider = providerRepository.save(provider);
            user.setProvider(provider);
            log.info("Registered PROVIDER account {} (provider id {}, status PENDING)", email, provider.getId());
        } else {
            CustomerProfile profile = customerProfileRepository.save(CustomerProfile.builder()
                    .user(user)
                    .defaultCity(request.city())
                    .build());
            user.setCustomerProfile(profile);
            log.info("Registered CUSTOMER account {}", email);
        }

        return new AuthResponseDTO(
                userMapper.toDto(user),
                role == UserRole.PROVIDER
                        ? "Provider account created. An administrator must approve your profile before you appear in search."
                        : "Account created. Welcome to Fixora.",
                false
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Email or password is incorrect."));

        // Plain-text comparison: development-only implementation.
        if (!request.password().equals(user.getPassword())) {
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("This account is deactivated. Contact support.");
        }

        log.info("Login succeeded for {}", email);
        return new AuthResponseDTO(
                userMapper.toDto(user),
                "Signed in (local development login — not a secure session).",
                false
        );
    }
}
