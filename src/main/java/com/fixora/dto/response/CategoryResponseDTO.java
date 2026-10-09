package com.fixora.dto.response;

public record CategoryResponseDTO(
        Long id,
        String name,
        String slug,
        String description,
        String iconName,
        String imageUrl,
        Boolean active,
        Integer displayOrder,
        long serviceCount
) {
}
