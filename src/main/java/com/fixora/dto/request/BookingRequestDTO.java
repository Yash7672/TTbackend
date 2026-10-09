package com.fixora.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Booking creation payload.
 *
 * NOTE: there is deliberately no price field. The server calculates the price
 * from the selected package — a client-supplied price is never trusted.
 */
public record BookingRequestDTO(

        @NotNull(message = "packageId is required")
        Long packageId,

        /** Optional. When omitted the booking is left unassigned for an admin. */
        Long providerId,

        @NotNull(message = "addressId is required")
        Long addressId,

        @NotNull(message = "bookingDate is required")
        @FutureOrPresent(message = "Booking date cannot be in the past")
        LocalDate bookingDate,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        /** Optional override; defaults to the package duration. Validated on the server. */
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 720, message = "Duration must be at most 720 minutes")
        Integer durationMinutes,

        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String customerNotes
) {
}
