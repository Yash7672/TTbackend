package com.fixora.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One weekly working window, e.g. MONDAY 09:00-18:00. */
public record AvailabilityRequestDTO(

        @NotNull(message = "dayOfWeek is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime,

        Boolean active
) {
}
