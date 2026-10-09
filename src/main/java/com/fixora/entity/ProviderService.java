package com.fixora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Which services a provider offers, with an optional provider-specific price. */
@Entity
@Table(
        name = "provider_services",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_provider_services_provider_service",
                columnNames = {"provider_id", "service_id"}
        ),
        indexes = @Index(name = "idx_provider_services_provider", columnList = "provider_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_services_provider"))
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, foreignKey = @ForeignKey(name = "fk_provider_services_service"))
    private Service service;

    /** Optional override of the catalogue price. Null means "use the package price". */
    @Column(name = "custom_price", precision = 10, scale = 2)
    private BigDecimal customPrice;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProviderService ps)) return false;
        return id != null && id.equals(ps.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
