package com.fixora.controller;

import com.fixora.dto.request.BookingRequestDTO;
import com.fixora.dto.request.BookingStatusUpdateRequestDTO;
import com.fixora.dto.response.BookingResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.enums.BookingStatus;
import com.fixora.enums.UserRole;
import com.fixora.repository.UserRepository;
import com.fixora.service.BookingService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDTO create(@CurrentUserId Long customerId,
                                     @Valid @RequestBody BookingRequestDTO request) {
        return bookingService.createBooking(customerId, request);
    }

    @GetMapping("/bookings/me")
    public PageResponseDTO<BookingResponseDTO> myBookings(@CurrentUserId Long customerId,
                                                          @RequestParam(required = false) BookingStatus status,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return bookingService.myBookings(customerId, status, page, size);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponseDTO get(@CurrentUserId Long userId, @PathVariable Long id) {
        return bookingService.getBooking(id, userId, roleOf(userId));
    }

    @PutMapping("/bookings/{id}/cancel")
    public BookingResponseDTO cancel(@CurrentUserId Long userId,
                                     @PathVariable Long id,
                                     @RequestBody(required = false) BookingStatusUpdateRequestDTO request) {
        return bookingService.cancel(id, userId, roleOf(userId), request == null ? null : request.note());
    }

    // ------------------------------------------- provider transitions

    @PutMapping("/bookings/{id}/accept")
    public BookingResponseDTO accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return bookingService.accept(userId, id);
    }

    @PutMapping("/bookings/{id}/reject")
    public BookingResponseDTO reject(@CurrentUserId Long userId,
                                     @PathVariable Long id,
                                     @RequestBody(required = false) BookingStatusUpdateRequestDTO request) {
        return bookingService.reject(userId, id, request == null ? null : request.note());
    }

    @PutMapping("/bookings/{id}/start")
    public BookingResponseDTO start(@CurrentUserId Long userId, @PathVariable Long id) {
        return bookingService.start(userId, id);
    }

    @PutMapping("/bookings/{id}/complete")
    public BookingResponseDTO complete(@CurrentUserId Long userId, @PathVariable Long id) {
        return bookingService.complete(userId, id);
    }

    /** The provider's own booking queue lives under /api/provider/bookings. */
    @GetMapping("/provider/bookings")
    public PageResponseDTO<BookingResponseDTO> providerBookings(@CurrentUserId Long userId,
                                                                @RequestParam(required = false) BookingStatus status,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return bookingService.providerBookings(userId, status, page, size);
    }

    private UserRole roleOf(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole())
                .orElse(UserRole.CUSTOMER);
    }
}
