package com.fixora.repository;

import com.fixora.entity.ProviderService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderServiceRepository extends JpaRepository<ProviderService, Long> {

    List<ProviderService> findByProviderIdOrderByIdAsc(Long providerId);

    Optional<ProviderService> findByProviderIdAndServiceId(Long providerId, Long serviceId);

    boolean existsByProviderIdAndServiceId(Long providerId, Long serviceId);

    void deleteByProviderIdAndServiceId(Long providerId, Long serviceId);

    boolean existsByServiceId(Long serviceId);

    /** One grouped count for a page of providers (avoids N+1). */
    @Query("""
            select ps.provider.id, count(ps) from ProviderService ps
            where ps.active = true and ps.provider.id in :providerIds
            group by ps.provider.id
            """)
    List<Object[]> countActiveByProviderIds(@Param("providerIds") List<Long> providerIds);

    /** Approved providers that offer a given service. */
    @Query("""
            select ps from ProviderService ps
            join fetch ps.provider p
            where ps.service.id = :serviceId
              and ps.active = true
              and p.status = com.fixora.enums.ProviderStatus.APPROVED
            """)
    List<ProviderService> findApprovedProvidersForService(@Param("serviceId") Long serviceId);
}
