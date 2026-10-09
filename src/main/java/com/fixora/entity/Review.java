package com.fixora.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A review. Exactly one review per completed booking (enforced by a unique
 * constraint on booking_id and re-checked in the service layer).
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_booking", columnNames = "booking_id"),
        indexes = {
                @Index(name = "idx_reviews_provider", columnList = "provider_id"),
                @Index(name = "idx_reviews_service", columnList = "service_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_booking"))
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_customer"))
    private User customer;

    /** Nullable because a booking may be completed with no provider assigned. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", foreignKey = @ForeignKey(name = "fk_reviews_provider"))
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_service"))
    private Service service;

    /** 1..5, validated on the server. */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** Admin moderation flag. Hidden reviews stay in the database but are not listed. */
    @Builder.Default
    @Column(name = "visible", nullable = false)
    private Boolean visible = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Review review)) return false;
        return id != null && id.equals(review.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
