package com.fixora.service;

import com.fixora.dto.request.BookingRequestDTO;
import com.fixora.dto.response.BookingResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.enums.BookingStatus;
import com.fixora.enums.UserRole;

public interface BookingService {

    BookingResponseDTO createBooking(Long customerId, BookingRequestDTO request);

    BookingResponseDTO getBooking(Long bookingId, Long userId, UserRole role);

    PageResponseDTO<BookingResponseDTO> myBookings(Long customerId, BookingStatus status, int page, int size);

    PageResponseDTO<BookingResponseDTO> providerBookings(Long userId, BookingStatus status, int page, int size);

    PageResponseDTO<BookingResponseDTO> allBookings(BookingStatus status, int page, int size);

    BookingResponseDTO accept(Long userId, Long bookingId);

    BookingResponseDTO reject(Long userId, Long bookingId, String note);

    BookingResponseDTO start(Long userId, Long bookingId);

    BookingResponseDTO complete(Long userId, Long bookingId);

    BookingResponseDTO cancel(Long bookingId, Long userId, UserRole role, String note);

    BookingResponseDTO assignProvider(Long bookingId, Long providerId);
}
