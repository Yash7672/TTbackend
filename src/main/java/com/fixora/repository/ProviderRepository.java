package com.fixora.repository;

import com.fixora.entity.Provider;
import com.fixora.entity.User;
import com.fixora.enums.ProviderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUser(User user);

    Optional<Provider> findByUserId(Long userId);

    List<Provider> findByStatus(ProviderStatus status);

    Page<Provider> findByStatus(ProviderStatus status, Pageable pageable);

    long countByStatus(ProviderStatus status);

    /**
     * Public provider search. Only APPROVED providers are ever returned, and the
     * filters are applied by the database.
     */
    @Query("""
            select p from Provider p
            where p.status = com.fixora.enums.ProviderStatus.APPROVED
              and (:city is null or lower(p.city) = lower(:city))
              and (:area is null or lower(p.area) like lower(concat('%', :area, '%')))
              and (:keyword is null
                   or lower(p.businessName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(p.bio, '')) like lower(concat('%', :keyword, '%')))
              and (:serviceId is null or exists (
                     select 1 from ProviderService ps
                     where ps.provider = p and ps.service.id = :serviceId and ps.active = true))
            """)
    Page<Provider> search(@Param("city") String city,
                          @Param("area") String area,
                          @Param("keyword") String keyword,
                          @Param("serviceId") Long serviceId,
                          Pageable pageable);
}
