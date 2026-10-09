package com.fixora.controller;

import com.fixora.dto.response.NotificationResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.service.NotificationService;
import com.fixora.validation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * In-app notifications. Nothing is emailed, texted or pushed: no notification
 * provider is configured, and the UI labels these as in-app notices.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponseDTO<NotificationResponseDTO> list(@CurrentUserId Long userId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(userId, page, size);
    }

    @GetMapping("/recent")
    public List<NotificationResponseDTO> recent(@CurrentUserId Long userId) {
        return notificationService.recent(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(@CurrentUserId Long userId) {
        return Map.of("unread", notificationService.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public NotificationResponseDTO markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        return notificationService.markRead(id, userId);
    }
}
