package com.fixora.mapper;

import com.fixora.dto.response.NotificationResponseDTO;
import com.fixora.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponseDTO toDto(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                Boolean.TRUE.equals(notification.getReadFlag()),
                notification.getRelatedBookingId(),
                notification.getCreatedAt()
        );
    }
}
