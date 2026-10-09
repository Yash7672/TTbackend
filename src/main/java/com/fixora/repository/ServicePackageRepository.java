package com.fixora.repository;

import com.fixora.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    List<ServicePackage> findByServiceIdOrderByDisplayOrderAscPriceAsc(Long serviceId);

    List<ServicePackage> findByServiceIdAndActiveTrueOrderByDisplayOrderAscPriceAsc(Long serviceId);

    /** Used by booking validation: the package must belong to the chosen service. */
    Optional<ServicePackage> findByIdAndServiceId(Long id, Long serviceId);

    long countByServiceId(Long serviceId);

    /** One grouped count for a page of services (avoids N+1). */
    @Query("""
            select p.service.id, count(p) from ServicePackage p
            where p.active = true and p.service.id in :serviceIds
            group by p.service.id
            """)
    List<Object[]> countActiveByServiceIds(@Param("serviceIds") List<Long> serviceIds);

    void deleteByServiceId(Long serviceId);
}
