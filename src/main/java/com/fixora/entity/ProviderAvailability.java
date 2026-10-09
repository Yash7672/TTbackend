package com.fixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A weekly working window for a provider, e.g. MONDAY 09:00-18:00. */
@Entity
@Table(
        name = "provider_availability",
        indexes = @Index(name = "idx_provider_availability_provider", columnList = "provider_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_availability_provider"))
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProviderAvailability pa)) return false;
        return id != null && id.equals(pa.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
