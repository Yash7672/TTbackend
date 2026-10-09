package com.fixora.service.impl;

import com.fixora.dto.request.UpdateUserRequestDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.dto.response.UserResponseDTO;
import com.fixora.entity.CustomerProfile;
import com.fixora.entity.User;
import com.fixora.enums.UserRole;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.mapper.UserMapper;
import com.fixora.repository.CustomerProfileRepository;
import com.fixora.repository.UserRepository;
import com.fixora.service.UserService;
import com.fixora.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getById(Long userId) {
        return userMapper.toDto(loadUser(userId));
    }

    @Override
    @Transactional
    public UserResponseDTO updateMe(Long userId, UpdateUserRequestDTO request) {
        User user = loadUser(userId);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());

        if (request.city() != null && !request.city().isBlank()) {
            CustomerProfile profile = user.getCustomerProfile();
            if (profile == null && user.getRole() != UserRole.ADMIN) {
                profile = customerProfileRepository.save(CustomerProfile.builder().user(user).build());
                user.setCustomerProfile(profile);
            }
            if (profile != null) {
                profile.setDefaultCity(request.city().trim());
                customerProfileRepository.save(profile);
            }
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> listUsers(UserRole role, int page, int size) {
        var pageable = PageUtils.of(page, size);
        var result = role == null
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);
        return PageResponseDTO.of(result, userMapper::toDto);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
