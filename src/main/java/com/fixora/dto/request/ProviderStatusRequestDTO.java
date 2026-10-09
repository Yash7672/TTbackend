package com.fixora.dto.request;

import com.fixora.enums.ProviderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin verification decision for a provider. */
public record ProviderStatusRequestDTO(

        @NotNull(message = "status is required")
        ProviderStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
