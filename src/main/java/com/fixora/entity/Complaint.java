package com.fixora.entity;

import com.fixora.enums.ComplaintCategory;
import com.fixora.enums.ComplaintPriority;
import com.fixora.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** A customer complaint. AI may suggest a category/priority; an admin decides. */
@Entity
@Table(
        name = "complaints",
        indexes = {
                @Index(name = "idx_complaints_customer", columnList = "customer_id"),
                @Index(name = "idx_complaints_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_complaints_customer"))
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", foreignKey = @ForeignKey(name = "fk_complaints_booking"))
    private Booking booking;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Final, admin-owned classification. */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ComplaintStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private ComplaintPriority priority;

    /** AI suggestion only — never applied automatically. */
    @Column(name = "ai_suggested_category", length = 30)
    private String aiSuggestedCategory;

    @Column(name = "ai_suggested_summary", columnDefinition = "TEXT")
    private String aiSuggestedSummary;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Complaint complaint)) return false;
        return id != null && id.equals(complaint.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
