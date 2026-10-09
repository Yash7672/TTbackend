package com.fixora.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** Extra customer-only data attached to a {@link User} with the CUSTOMER role. */
@Entity
@Table(
        name = "customer_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_profiles_user", columnNames = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_customer_profiles_user"))
    private User user;

    /** Last city the customer selected on the storefront (used for discovery). */
    @Column(name = "default_city", length = 80)
    private String defaultCity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CustomerProfile profile)) return false;
        return id != null && id.equals(profile.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
