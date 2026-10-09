package com.fixora.entity;

import com.fixora.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** A service location saved by a customer. */
@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_addresses_customer", columnList = "customer_id"),
                @Index(name = "idx_addresses_city", columnList = "city")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner. Always re-checked on the server; never trusted from the client. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_addresses_customer"))
    private User customer;

    @Column(name = "label", length = 60)
    private String label;

    @Column(name = "line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "line2", length = 200)
    private String line2;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "area", length = 120)
    private String area;

    @Column(name = "pincode", length = 12)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean defaultAddress = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Address address)) return false;
        return id != null && id.equals(address.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
