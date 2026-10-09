package com.fixora.service.impl;

import com.fixora.dto.request.BookingRequestDTO;
import com.fixora.dto.response.BookingResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.entity.*;
import com.fixora.enums.*;
import com.fixora.exception.*;
import com.fixora.mapper.BookingMapper;
import com.fixora.repository.*;
import com.fixora.service.BookingService;
import com.fixora.service.NotificationService;
import com.fixora.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final AddressRepository addressRepository;
    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------ create

    @Override
    @Transactional
    public BookingResponseDTO createBooking(Long customerId, BookingRequestDTO request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", customerId));

        // 1 + 2 + 3: package exists, is active, and belongs to an active service.
        ServicePackage pkg = packageRepository.findById(request.packageId())
                .orElseThrow(() -> new BadRequestException(
                        "Package " + request.packageId() + " does not exist."));
        if (!Boolean.TRUE.equals(pkg.getActive())) {
            throw new BadRequestException("The selected package is not available for booking.");
        }
        Service service = pkg.getService();
        if (service == null || !Boolean.TRUE.equals(service.getActive())) {
            throw new BadRequestException("The selected service is not currently offered.");
        }

        // 7: the address must belong to the customer.
        Address address = addressRepository.findByIdAndCustomerId(request.addressId(), customerId)
                .orElseThrow(() -> new BadRequestException(
                        "Address " + request.addressId() + " does not belong to your account."));

        // 8: no bookings in the past.
        LocalDate date = request.bookingDate();
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past.");
        }

        // 9: valid time window.
        LocalTime start = request.startTime();
        int duration = request.durationMinutes() != null
                ? request.durationMinutes()
                : pkg.getDurationMinutes();
        if (duration < 15 || duration > 720) {
            throw new BadRequestException("Duration must be between 15 and 720 minutes.");
        }
        LocalTime end = start.plusMinutes(duration);
        if (!end.isAfter(start)) {
            throw new BadRequestException("The service must finish on the same day it starts.");
        }

        // 4 + 5 + 6 + 10 + 11: provider checks (skipped when no provider is chosen).
        Provider provider = null;
        if (request.providerId() != null) {
            provider = providerRepository.findById(request.providerId())
                    .orElseThrow(() -> new BadRequestException(
                            "Provider " + request.providerId() + " does not exist."));

            if (provider.getStatus() != ProviderStatus.APPROVED) {
                throw new BadRequestException(
                        "The selected provider is not approved for new bookings yet.");
            }
            if (!providerServiceRepository.existsByProviderIdAndServiceId(provider.getId(), service.getId())) {
                throw new BadRequestException("The selected provider does not offer " + service.getName() + ".");
            }

            boolean covered = availabilityRepository
                    .findByProviderIdAndDayOfWeekAndActiveTrue(provider.getId(), date.getDayOfWeek())
                    .stream()
                    .anyMatch(w -> !start.isBefore(w.getStartTime()) && !end.isAfter(w.getEndTime()));
            if (!covered) {
                throw new BadRequestException("The provider is not available on "
                        + date.getDayOfWeek() + " between " + start + " and " + end + ".");
            }

            if (bookingRepository.countOverlapping(provider.getId(), date, start, end) > 0) {
                throw new BookingConflictException(
                        "That provider already has a booking overlapping this time slot. Pick another time or provider.");
            }
        }

        // 12: the price always comes from the package — never from the client.
        BigDecimal agreedPrice = pkg.getPrice();

        Booking booking = Booking.builder()
                .bookingRef(generateReference())
                .customer(customer)
                .provider(provider)
                .service(service)
                .servicePackage(pkg)
                .packageNameSnapshot(pkg.getName())
                .agreedPrice(agreedPrice)
                .address(address)
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .customerNotes(request.customerNotes())
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        booking.addHistory(BookingStatusHistory.builder()
                .status(BookingStatus.PENDING)
                .note(provider == null
                        ? "Booking created — waiting for a provider to be assigned."
                        : "Booking created — waiting for provider response.")
                .build());

        booking = bookingRepository.save(booking);

        notificationService.notifyBooking(booking, NotificationType.BOOKING_CREATED,
                "Booking " + booking.getBookingRef() + " created",
                service.getName() + " (" + pkg.getName() + ") on " + date + " at " + start + ".");

        log.info("Booking {} created by customer {} for package {}", booking.getBookingRef(), customerId, pkg.getId());
        return bookingMapper.toDto(booking, false);
    }

    // ------------------------------------------------------------------- read

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBooking(Long bookingId, Long userId, UserRole role) {
        Booking booking = loadBooking(bookingId);
        assertCanView(booking, userId, role);
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BookingResponseDTO> myBookings(Long customerId, BookingStatus status, int page, int size) {
        Page<Booking> result = status == null
                ? bookingRepository.findByCustomerIdOrderByBookingDateDescStartTimeDesc(customerId, PageUtils.of(page, size))
                : bookingRepository.findByCustomerIdAndStatusOrderByBookingDateDesc(customerId, status, PageUtils.of(page, size));
        return toPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BookingResponseDTO> providerBookings(Long userId, BookingStatus status, int page, int size) {
        Provider provider = requireProviderProfile(userId);
        Page<Booking> result = status == null
                ? bookingRepository.findByProviderIdOrderByBookingDateDescStartTimeDesc(provider.getId(), PageUtils.of(page, size))
                : bookingRepository.findByProviderIdAndStatusOrderByBookingDateAscStartTimeAsc(provider.getId(), status, PageUtils.of(page, size));
        return toPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BookingResponseDTO> allBookings(BookingStatus status, int page, int size) {
        var pageable = PageUtils.of(page, size, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "bookingDate"));
        Page<Booking> result = status == null
                ? bookingRepository.findAll(pageable)
                : bookingRepository.findByStatusOrderByBookingDateDescStartTimeDesc(status, pageable);
        return toPage(result);
    }

    // -------------------------------------------------------------- transitions

    @Override
    @Transactional
    public BookingResponseDTO accept(Long userId, Long bookingId) {
        Booking booking = loadBooking(bookingId);
        requireAssignedProvider(userId, booking);
        transition(booking, BookingStatus.ACCEPTED, "Provider accepted the booking.");
        notificationService.notifyBooking(booking, NotificationType.BOOKING_ACCEPTED,
                "Booking " + booking.getBookingRef() + " accepted",
                "Your provider accepted the booking scheduled for " + booking.getBookingDate() + ".");
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDTO reject(Long userId, Long bookingId, String note) {
        Booking booking = loadBooking(bookingId);
        requireAssignedProvider(userId, booking);
        transition(booking, BookingStatus.REJECTED,
                note == null || note.isBlank() ? "Provider rejected the booking." : note);
        notificationService.notifyBooking(booking, NotificationType.BOOKING_REJECTED,
                "Booking " + booking.getBookingRef() + " rejected",
                "The provider could not take this booking. Reason: " + booking.getStatusNote());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDTO start(Long userId, Long bookingId) {
        Booking booking = loadBooking(bookingId);
        requireAssignedProvider(userId, booking);
        transition(booking, BookingStatus.IN_PROGRESS, "Provider started the service.");
        notificationService.notifyBooking(booking, NotificationType.SERVICE_STARTED,
                "Service started for " + booking.getBookingRef(),
                "Your provider has started the service.");
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDTO complete(Long userId, Long bookingId) {
        Booking booking = loadBooking(bookingId);
        requireAssignedProvider(userId, booking);
        transition(booking, BookingStatus.COMPLETED, "Provider marked the service complete.");
        notificationService.notifyUser(booking.getCustomer(), NotificationType.SERVICE_COMPLETED,
                "Service completed for " + booking.getBookingRef(),
                "Thanks for using Fixora. You can now review this service.", booking.getId());
        notificationService.notifyUser(booking.getCustomer(), NotificationType.REVIEW_REMINDER,
                "Leave a review for " + booking.getBookingRef(),
                "Your review helps other customers pick the right provider.", booking.getId());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDTO cancel(Long bookingId, Long userId, UserRole role, String note) {
        Booking booking = loadBooking(bookingId);

        if (role == UserRole.CUSTOMER && !booking.getCustomer().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only cancel your own bookings.");
        }
        if (role == UserRole.PROVIDER) {
            requireAssignedProvider(userId, booking);
        }
        if (role == UserRole.CUSTOMER && booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("A booking in the past can no longer be cancelled.");
        }

        transition(booking, BookingStatus.CANCELLED,
                note == null || note.isBlank() ? "Cancelled." : note);
        notificationService.notifyBooking(booking, NotificationType.BOOKING_CANCELLED,
                "Booking " + booking.getBookingRef() + " cancelled",
                "This booking was cancelled. Reason: " + booking.getStatusNote());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDTO assignProvider(Long bookingId, Long providerId) {
        Booking booking = loadBooking(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingTransitionException(
                    "Only PENDING bookings can be assigned to a provider.");
        }

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider", providerId));
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw new BadRequestException("Only approved providers can take new bookings.");
        }
        if (!providerServiceRepository.existsByProviderIdAndServiceId(providerId, booking.getService().getId())) {
            throw new BadRequestException("That provider does not offer " + booking.getService().getName() + ".");
        }
        if (bookingRepository.countOverlapping(providerId, booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime()) > 0) {
            throw new BookingConflictException("That provider already has an overlapping booking.");
        }

        booking.setProvider(provider);
        booking.addHistory(BookingStatusHistory.builder()
                .status(BookingStatus.PENDING)
                .note("Assigned to provider " + provider.getBusinessName() + " by an administrator.")
                .build());
        bookingRepository.save(booking);

        notificationService.notifyUser(provider.getUser(), NotificationType.BOOKING_CREATED,
                "New booking " + booking.getBookingRef(),
                "You have a new booking request for " + booking.getBookingDate() + ".", booking.getId());

        return bookingMapper.toDto(booking);
    }

    // ----------------------------------------------------------------- helpers

    /**
     * The single place where booking status changes happen, so the allowed
     * transition table cannot be bypassed anywhere else in the code base.
     */
    private void transition(Booking booking, BookingStatus target, String note) {
        BookingStatus current = booking.getStatus();
        boolean allowed = switch (current) {
            case PENDING -> target == BookingStatus.ACCEPTED
                    || target == BookingStatus.REJECTED
                    || target == BookingStatus.CANCELLED;
            case ACCEPTED -> target == BookingStatus.IN_PROGRESS
                    || target == BookingStatus.CANCELLED;
            case IN_PROGRESS -> target == BookingStatus.COMPLETED;
            case COMPLETED, REJECTED, CANCELLED -> false;
        };

        if (!allowed) {
            throw InvalidBookingTransitionException.between(current.name(), target.name());
        }

        booking.setStatus(target);
        booking.setStatusNote(note);
        booking.addHistory(BookingStatusHistory.builder().status(target).note(note).build());
        bookingRepository.save(booking);
    }

    private Booking loadBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
    }

    private void assertCanView(Booking booking, Long userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return;
        }
        boolean isOwner = booking.getCustomer() != null && booking.getCustomer().getId().equals(userId);
        boolean isProvider = booking.getProvider() != null
                && booking.getProvider().getUser() != null
                && booking.getProvider().getUser().getId().equals(userId);
        if (!isOwner && !isProvider) {
            throw new UnauthorizedActionException("This booking does not belong to you.");
        }
    }

    /** A provider action is only valid for the provider actually assigned to the booking. */
    private void requireAssignedProvider(Long userId, Booking booking) {
        Provider provider = requireProviderProfile(userId);
        if (booking.getProvider() == null || !booking.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("This booking is not assigned to you.");
        }
    }

    private Provider requireProviderProfile(Long userId) {
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedActionException(
                        "This account does not have a provider profile."));
    }

    private PageResponseDTO<BookingResponseDTO> toPage(Page<Booking> result) {
        List<BookingResponseDTO> content = bookingMapper.toDtoList(result.getContent());
        return new PageResponseDTO<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    private String generateReference() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "FX-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            if (bookingRepository.findByBookingRef(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique booking reference.");
    }
}
