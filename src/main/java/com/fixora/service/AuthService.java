package com.fixora.service;

import com.fixora.dto.request.LoginRequestDTO;
import com.fixora.dto.request.RegisterRequestDTO;
import com.fixora.dto.response.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);
}
