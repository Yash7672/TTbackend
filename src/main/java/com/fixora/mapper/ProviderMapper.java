package com.fixora.mapper;

import com.fixora.dto.response.AvailabilityResponseDTO;
import com.fixora.dto.response.ProviderResponseDTO;
import com.fixora.dto.response.ProviderServiceResponseDTO;
import com.fixora.entity.Provider;
import com.fixora.entity.ProviderAvailability;
import com.fixora.entity.ProviderService;
import com.fixora.repository.ProviderServiceRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderMapper {

    private final ProviderServiceRepository providerServiceRepository;

    public ProviderMapper(ProviderServiceRepository providerServiceRepository) {
        this.providerServiceRepository = providerServiceRepository;
    }

    public ProviderResponseDTO toDto(Provider provider, long serviceCount) {
        return new ProviderResponseDTO(
                provider.getId(),
                provider.getUser() != null ? provider.getUser().getId() : null,
                provider.getBusinessName(),
                provider.getBio(),
                provider.getCity(),
                provider.getArea(),
                provider.getExperienceYears(),
                provider.getStatus(),
                provider.getRatingAverage(),
                provider.getRatingCount(),
                provider.getProfileImageUrl(),
                provider.getUser() != null ? provider.getUser().getFullName() : null,
                serviceCount
        );
    }

    public ProviderResponseDTO toDto(Provider provider) {
        return toDto(provider, providerServiceRepository.findByProviderIdOrderByIdAsc(provider.getId()).size());
    }

    /** Single grouped count query for a whole page of providers. */
    public List<ProviderResponseDTO> toDtoList(List<Provider> providers) {
        Map<Long, Long> counts = new HashMap<>();
        if (!providers.isEmpty()) {
            List<Long> ids = providers.stream().map(Provider::getId).toList();
            for (Object[] row : providerServiceRepository.countActiveByProviderIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return providers.stream()
                .map(p -> toDto(p, counts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    public ProviderServiceResponseDTO toDto(ProviderService ps) {
        return new ProviderServiceResponseDTO(
                ps.getId(),
                ps.getProvider() != null ? ps.getProvider().getId() : null,
                ps.getService() != null ? ps.getService().getId() : null,
                ps.getService() != null ? ps.getService().getName() : null,
                ps.getService() != null && ps.getService().getCategory() != null
                        ? ps.getService().getCategory().getName() : null,
                ps.getCustomPrice(),
                ps.getService() != null ? ps.getService().getBasePrice() : null,
                ps.getActive()
        );
    }

    public AvailabilityResponseDTO toDto(ProviderAvailability availability) {
        return new AvailabilityResponseDTO(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getActive()
        );
    }
}
