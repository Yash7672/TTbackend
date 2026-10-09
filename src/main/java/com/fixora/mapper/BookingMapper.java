package com.fixora.mapper;

import com.fixora.dto.response.BookingResponseDTO;
import com.fixora.dto.response.BookingStatusHistoryResponseDTO;
import com.fixora.entity.Booking;
import com.fixora.enums.BookingStatus;
import com.fixora.repository.ReviewRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class BookingMapper {

    private final AddressMapper addressMapper;
    private final ReviewRepository reviewRepository;

    public BookingMapper(AddressMapper addressMapper, ReviewRepository reviewRepository) {
        this.addressMapper = addressMapper;
        this.reviewRepository = reviewRepository;
    }

    public BookingResponseDTO toDto(Booking booking, boolean hasReview) {
        BookingStatus status = booking.getStatus();

        // Cancellation is only offered while the booking can still be stopped.
        boolean canCancel = (status == BookingStatus.PENDING || status == BookingStatus.ACCEPTED)
                && !booking.getBookingDate().isBefore(LocalDate.now());

        // A review requires a COMPLETED booking that does not have one yet.
        boolean canReview = status == BookingStatus.COMPLETED && !hasReview;

        List<BookingStatusHistoryResponseDTO> history = booking.getStatusHistory().stream()
                .sorted(Comparator.comparing(h -> h.getChangedAt() == null ? java.time.Instant.EPOCH : h.getChangedAt()))
                .map(h -> new BookingStatusHistoryResponseDTO(h.getStatus(), h.getNote(), h.getChangedAt()))
                .toList();

        return new BookingResponseDTO(
                booking.getId(),
                booking.getBookingRef(),
                status,
                booking.getPaymentStatus(),
                booking.getCustomer() != null ? booking.getCustomer().getId() : null,
                booking.getCustomer() != null ? booking.getCustomer().getFullName() : null,
                booking.getCustomer() != null ? booking.getCustomer().getEmail() : null,
                booking.getProvider() != null ? booking.getProvider().getId() : null,
                booking.getProvider() != null ? booking.getProvider().getBusinessName() : null,
                booking.getService() != null ? booking.getService().getId() : null,
                booking.getService() != null ? booking.getService().getName() : null,
                booking.getServicePackage() != null ? booking.getServicePackage().getId() : null,
                booking.getPackageNameSnapshot(),
                booking.getAgreedPrice(),
                booking.getAddress() != null ? booking.getAddress().getId() : null,
                addressMapper.summarise(booking.getAddress()),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getCustomerNotes(),
                booking.getStatusNote(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                hasReview,
                canReview,
                canCancel,
                history
        );
    }

    public BookingResponseDTO toDto(Booking booking) {
        return toDto(booking, reviewRepository.existsByBookingId(booking.getId()));
    }

    /** Batched review lookup so listing bookings stays at two queries. */
    public List<BookingResponseDTO> toDtoList(List<Booking> bookings) {
        Set<Long> reviewed = new HashSet<>();
        if (!bookings.isEmpty()) {
            List<Long> ids = bookings.stream().map(Booking::getId).toList();
            reviewed.addAll(reviewRepository.findReviewedBookingIds(ids));
        }
        return bookings.stream()
                .map(b -> toDto(b, reviewed.contains(b.getId())))
                .toList();
    }
}
