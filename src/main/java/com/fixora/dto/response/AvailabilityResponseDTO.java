package com.fixora.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityResponseDTO(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean active
) {
}
