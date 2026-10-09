package com.fixora.entity;

import com.fixora.enums.ProviderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** A service professional. Only APPROVED providers are publicly listed. */
@Entity
@Table(
        name = "providers",
        uniqueConstraints = @UniqueConstraint(name = "uk_providers_user", columnNames = "user_id"),
        indexes = {
                @Index(name = "idx_providers_status", columnList = "status"),
                @Index(name = "idx_providers_city", columnList = "city")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_providers_user"))
    private User user;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "area", length = 120)
    private String area;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProviderStatus status;

    /** Average of real review ratings only. 0 when the provider has no reviews. */
    @Builder.Default
    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "profile_image_url", length = 400)
    private String profileImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Provider provider)) return false;
        return id != null && id.equals(provider.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
