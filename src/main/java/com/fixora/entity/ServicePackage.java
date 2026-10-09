package com.fixora.entity;

import com.fixora.enums.PricingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** A priced variant of a service, e.g. "Basic Inspection - Rs.199". */
@Entity
@Table(
        name = "service_packages",
        indexes = @Index(name = "idx_service_packages_service", columnList = "service_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, foreignKey = @ForeignKey(name = "fk_service_packages_service"))
    private Service service;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Authoritative price. The client never sends a price to the server. */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "included_work", columnDefinition = "TEXT")
    private String includedWork;

    @Column(name = "excluded_work", columnDefinition = "TEXT")
    private String excludedWork;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 25)
    private PricingType pricingType;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ServicePackage pkg)) return false;
        return id != null && id.equals(pkg.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
