package com.fixora.service;

import com.fixora.dto.response.NotificationResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.entity.Booking;
import com.fixora.entity.Notification;
import com.fixora.entity.User;
import com.fixora.enums.NotificationType;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.mapper.NotificationMapper;
import com.fixora.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Database-backed in-app notifications.
 *
 * Emails / SMS / push are NOT sent: no notification provider is configured, and
 * the UI says "in-app notification" rather than claiming delivery.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public void notifyUser(User user, NotificationType type, String title, String message, Long bookingId) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .readFlag(false)
                .relatedBookingId(bookingId)
                .build());
    }

    @Transactional
    public void notifyBooking(Booking booking, NotificationType type, String title, String message) {
        notifyUser(booking.getCustomer(), type, title, message, booking.getId());
        if (booking.getProvider() != null && booking.getProvider().getUser() != null) {
            notifyUser(booking.getProvider().getUser(), type, title, message, booking.getId());
        }
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<NotificationResponseDTO> list(Long userId, int page, int size) {
        var result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PageResponseDTO.of(result, notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> recent(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(notificationMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public NotificationResponseDTO markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new com.fixora.exception.UnauthorizedActionException(
                    "This notification does not belong to you.");
        }
        notification.setReadFlag(true);
        return notificationMapper.toDto(notificationRepository.save(notification));
    }
}
