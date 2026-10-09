package com.fixora.controller;

import com.fixora.dto.request.UpdateUserRequestDTO;
import com.fixora.dto.response.UserResponseDTO;
import com.fixora.service.UserService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDTO me(@CurrentUserId Long userId) {
        return userService.getById(userId);
    }

    @PutMapping("/me")
    public UserResponseDTO updateMe(@CurrentUserId Long userId,
                                    @Valid @RequestBody UpdateUserRequestDTO request) {
        return userService.updateMe(userId, request);
    }
}
