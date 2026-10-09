package com.fixora.dto.request;

import jakarta.validation.constraints.Size;

/** Optional note attached to a status change (reject / cancel reason). */
public record BookingStatusUpdateRequestDTO(

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
