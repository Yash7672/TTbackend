package com.fixora.dto.response;

import com.fixora.enums.BookingStatus;

import java.time.Instant;

public record BookingStatusHistoryResponseDTO(
        BookingStatus status,
        String note,
        Instant changedAt
) {
}
