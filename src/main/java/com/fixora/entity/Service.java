package com.fixora.entity;

import com.fixora.enums.PricingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A bookable service (e.g. "AC General Service") belonging to one category. */
@Entity
@Table(
        name = "services",
        uniqueConstraints = @UniqueConstraint(name = "uk_services_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_services_category", columnList = "category_id"),
                @Index(name = "idx_services_name", columnList = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_services_category"))
    private Category category;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    /** Demo price in INR. Displayed as "starting from" unless pricingType is FIXED_PRICE. */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 25)
    private PricingType pricingType;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "featured", nullable = false)
    private Boolean featured = false;

    /** Average of real reviews; 0 when there are no reviews yet ("No reviews yet"). */
    @Builder.Default
    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private List<ServicePackage> packages = new ArrayList<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Service service)) return false;
        return id != null && id.equals(service.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
