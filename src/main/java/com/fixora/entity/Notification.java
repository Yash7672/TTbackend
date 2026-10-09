package com.fixora.entity;

import com.fixora.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** In-app notification stored in MySQL. No email/SMS/push delivery is configured. */
@Entity
@Table(
        name = "notifications",
        indexes = @Index(name = "idx_notifications_user", columnList = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notifications_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean readFlag = false;

    /** Optional deep-link target, e.g. the booking id this notification is about. */
    @Column(name = "related_booking_id")
    private Long relatedBookingId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Notification notification)) return false;
        return id != null && id.equals(notification.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
