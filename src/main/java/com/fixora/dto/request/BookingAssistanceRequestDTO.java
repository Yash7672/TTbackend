package com.fixora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "I only need a basic AC check" — answered from real package data. */
public record BookingAssistanceRequestDTO(

        @NotBlank(message = "message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String message,

        /** Hint: when supplied, only this service's real packages are described. */
        Long serviceId,

        Long packageId
) {
}
