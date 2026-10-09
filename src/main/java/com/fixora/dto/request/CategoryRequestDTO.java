package com.fixora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Admin create/update payload for a category. */
public record CategoryRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 60, message = "Icon name must be at most 60 characters")
        String iconName,

        @Size(max = 400, message = "Image URL must be at most 400 characters")
        String imageUrl,

        Boolean active,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder
) {
}
