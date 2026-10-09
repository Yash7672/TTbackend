package com.fixora.service;

import com.fixora.dto.request.UpdateUserRequestDTO;
import com.fixora.dto.response.UserResponseDTO;
import com.fixora.enums.UserRole;
import com.fixora.dto.response.PageResponseDTO;

public interface UserService {

    UserResponseDTO getById(Long userId);

    UserResponseDTO updateMe(Long userId, UpdateUserRequestDTO request);

    PageResponseDTO<UserResponseDTO> listUsers(UserRole role, int page, int size);
}
