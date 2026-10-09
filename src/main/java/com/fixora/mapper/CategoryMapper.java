package com.fixora.mapper;

import com.fixora.dto.response.CategoryResponseDTO;
import com.fixora.entity.Category;
import com.fixora.repository.ServiceRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CategoryMapper {

    private final ServiceRepository serviceRepository;

    public CategoryMapper(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public CategoryResponseDTO toDto(Category category, long serviceCount) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIconName(),
                category.getImageUrl(),
                category.getActive(),
                category.getDisplayOrder(),
                serviceCount
        );
    }

    public CategoryResponseDTO toDto(Category category) {
        return toDto(category, serviceRepository.countByCategoryId(category.getId()));
    }

    /** One grouped query instead of one count per category (avoids N+1). */
    public List<CategoryResponseDTO> toDtoList(List<Category> categories) {
        Map<Long, Long> counts = new HashMap<>();
        if (!categories.isEmpty()) {
            List<Long> ids = categories.stream().map(Category::getId).toList();
            for (Object[] row : serviceRepository.countActiveServicesByCategoryIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return categories.stream()
                .map(c -> toDto(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }
}
