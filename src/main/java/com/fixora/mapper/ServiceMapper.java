package com.fixora.mapper;

import com.fixora.dto.response.ServicePackageResponseDTO;
import com.fixora.dto.response.ServiceResponseDTO;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.repository.ServicePackageRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ServiceMapper {

    private final ServicePackageRepository packageRepository;

    public ServiceMapper(ServicePackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public ServiceResponseDTO toDto(Service service, long packageCount) {
        return new ServiceResponseDTO(
                service.getId(),
                service.getName(),
                service.getSlug(),
                service.getShortDescription(),
                service.getDescription(),
                service.getCategory() != null ? service.getCategory().getId() : null,
                service.getCategory() != null ? service.getCategory().getName() : null,
                service.getCategory() != null ? service.getCategory().getSlug() : null,
                service.getImageUrl(),
                service.getBasePrice(),
                service.getPricingType(),
                service.getDurationMinutes(),
                service.getActive(),
                service.getFeatured(),
                service.getRatingAverage(),
                service.getRatingCount(),
                packageCount
        );
    }

    public ServiceResponseDTO toDto(Service service) {
        return toDto(service, packageRepository.countByServiceId(service.getId()));
    }

    /** One grouped query for the whole page instead of one query per service. */
    public List<ServiceResponseDTO> toDtoList(List<Service> services) {
        Map<Long, Long> counts = new HashMap<>();
        if (!services.isEmpty()) {
            List<Long> ids = services.stream().map(Service::getId).toList();
            for (Object[] row : packageRepository.countActiveByServiceIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return services.stream()
                .map(s -> toDto(s, counts.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    public ServicePackageResponseDTO toDto(ServicePackage pkg) {
        return new ServicePackageResponseDTO(
                pkg.getId(),
                pkg.getService() != null ? pkg.getService().getId() : null,
                pkg.getService() != null ? pkg.getService().getName() : null,
                pkg.getName(),
                pkg.getDescription(),
                pkg.getPrice(),
                pkg.getDurationMinutes(),
                pkg.getIncludedWork(),
                pkg.getExcludedWork(),
                pkg.getPricingType(),
                pkg.getActive(),
                pkg.getDisplayOrder()
        );
    }

    public List<ServicePackageResponseDTO> toPackageDtoList(List<ServicePackage> packages) {
        return packages.stream().map(this::toDto).toList();
    }
}
