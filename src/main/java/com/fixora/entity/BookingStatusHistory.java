package com.fixora.entity;

import com.fixora.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** Audit trail entry: which status a booking moved to, when, and why. */
@Entity
@Table(
        name = "booking_status_history",
        indexes = @Index(name = "idx_booking_history_booking", columnList = "booking_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_history_booking"))
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private Instant changedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BookingStatusHistory h)) return false;
        return id != null && id.equals(h.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
