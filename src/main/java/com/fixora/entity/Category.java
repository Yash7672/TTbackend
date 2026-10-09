package com.fixora.entity;

import jakarta.persistence.*;
import lombok.*;

/** A top-level service category shown on the storefront (e.g. "Home Cleaning"). */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categories_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, length = 140)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    /** Lucide icon name used by the frontend. */
    @Column(name = "icon_name", length = 60)
    private String iconName;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Category category)) return false;
        return id != null && id.equals(category.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
