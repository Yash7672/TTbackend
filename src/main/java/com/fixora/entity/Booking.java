package com.fixora.entity;

import com.fixora.enums.BookingStatus;
import com.fixora.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** A customer booking for one package of one service. */
@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookings_ref", columnNames = "booking_ref"),
        indexes = {
                @Index(name = "idx_bookings_customer", columnList = "customer_id"),
                @Index(name = "idx_bookings_provider", columnList = "provider_id"),
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_date", columnList = "booking_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human readable reference, e.g. FX-4F8A2C. */
    @Column(name = "booking_ref", nullable = false, length = 20)
    private String bookingRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_customer"))
    private User customer;

    /** Nullable: a booking may be created without choosing a provider yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", foreignKey = @ForeignKey(name = "fk_bookings_provider"))
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_service"))
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_package"))
    private ServicePackage servicePackage;

    /** Snapshot of the package name at booking time, for historical accuracy. */
    @Column(name = "package_name_snapshot", nullable = false, length = 150)
    private String packageNameSnapshot;

    /** Server-calculated price snapshot. A client-submitted price is never trusted. */
    @Column(name = "agreed_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_address"))
    private Address address;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    /** Reason supplied when a booking is rejected or cancelled. */
    @Column(name = "status_note", length = 500)
    private String statusNote;

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingStatusHistory> statusHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void addHistory(BookingStatusHistory history) {
        history.setBooking(this);
        this.statusHistory.add(history);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Booking booking)) return false;
        return id != null && id.equals(booking.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
