package com.fixora.dto.response;

import com.fixora.enums.BookingStatus;
import com.fixora.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BookingResponseDTO(
        Long id,
        String bookingRef,
        BookingStatus status,
        PaymentStatus paymentStatus,
        Long customerId,
        String customerName,
        String customerEmail,
        Long providerId,
        String providerName,
        Long serviceId,
        String serviceName,
        Long packageId,
        String packageName,
        BigDecimal agreedPrice,
        Long addressId,
        String addressSummary,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        String customerNotes,
        String statusNote,
        Instant createdAt,
        Instant updatedAt,
        boolean hasReview,
        boolean canReview,
        boolean canCancel,
        List<BookingStatusHistoryResponseDTO> statusHistory
) {
}
