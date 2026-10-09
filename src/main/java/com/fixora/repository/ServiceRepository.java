package com.fixora.repository;

import com.fixora.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    Optional<Service> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    Page<Service> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    List<Service> findTop8ByActiveTrueOrderByRatingCountDescRatingAverageDesc();

    List<Service> findByActiveTrueAndFeaturedTrueOrderByRatingAverageDescIdAsc();

    long countByActiveTrue();

    /** Used by the deterministic AI matcher: narrow candidates by name. */
    List<Service> findByActiveTrueAndNameContainingIgnoreCase(String namePart);

    /**
     * Catalogue search executed by the database. Every filter is optional; a null
     * value simply disables that filter. Never returns a hardcoded list.
     */
    @Query("""
            select s from Service s
            where s.active = true
              and (:categoryId is null or s.category.id = :categoryId)
              and (:keyword is null
                   or lower(s.name) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(s.shortDescription, '')) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or s.basePrice >= :minPrice)
              and (:maxPrice is null or s.basePrice <= :maxPrice)
            """)
    Page<Service> search(@Param("categoryId") Long categoryId,
                         @Param("keyword") String keyword,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);

    /** One grouped count for a list of categories (avoids N+1 on the storefront). */
    @Query("""
            select s.category.id, count(s) from Service s
            where s.active = true and s.category.id in :categoryIds
            group by s.category.id
            """)
    List<Object[]> countActiveServicesByCategoryIds(@Param("categoryIds") List<Long> categoryIds);
}
