package com.fixora.service.impl;

import com.fixora.dto.request.AvailabilityRequestDTO;
import com.fixora.dto.request.ProviderRequestDTO;
import com.fixora.dto.request.ProviderServiceRequestDTO;
import com.fixora.dto.response.*;
import com.fixora.entity.*;
import com.fixora.enums.BookingStatus;
import com.fixora.enums.ProviderStatus;
import com.fixora.enums.UserRole;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.DuplicateResourceException;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.exception.UnauthorizedActionException;
import com.fixora.mapper.ProviderMapper;
import com.fixora.repository.*;
import com.fixora.service.NotificationService;
import com.fixora.service.ProviderManager;
import com.fixora.util.PageUtils;
import com.fixora.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ProviderManagerImpl implements ProviderManager {

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ProviderMapper providerMapper;
    private final NotificationService notificationService;

    // ----------------------------------------------------------------- profile

    @Override
    @Transactional
    public ProviderResponseDTO createMyProfile(Long userId, ProviderRequestDTO request) {
        User user = loadUser(userId);
        if (providerRepository.findByUserId(userId).isPresent()) {
            throw new DuplicateResourceException("This account already has a provider profile.");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Administrator accounts cannot become providers.");
        }

        Provider provider = Provider.builder()
                .user(user)
                .businessName(request.businessName().trim())
                .bio(request.bio())
                .city(request.city().trim())
                .area(request.area())
                .experienceYears(request.experienceYears())
                .profileImageUrl(request.profileImageUrl())
                .status(ProviderStatus.PENDING)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .build();

        provider = providerRepository.save(provider);
        user.setRole(UserRole.PROVIDER);
        user.setProvider(provider);
        userRepository.save(user);

        log.info("Provider profile {} created for user {} (status PENDING)", provider.getId(), userId);
        return providerMapper.toDto(provider, 0);
    }

    @Override
    @Transactional
    public ProviderResponseDTO updateMyProfile(Long userId, ProviderRequestDTO request) {
        Provider provider = requireProvider(userId);

        provider.setBusinessName(request.businessName().trim());
        provider.setBio(request.bio());
        provider.setCity(request.city().trim());
        provider.setArea(request.area());
        provider.setExperienceYears(request.experienceYears());
        provider.setProfileImageUrl(request.profileImageUrl());

        // A REJECTED or SUSPENDED profile goes back into the review queue once edited.
        if (provider.getStatus() == ProviderStatus.REJECTED || provider.getStatus() == ProviderStatus.SUSPENDED) {
            provider.setStatus(ProviderStatus.PENDING);
        }

        return providerMapper.toDto(providerRepository.save(provider));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponseDTO getMyProfile(Long userId) {
        return providerMapper.toDto(requireProvider(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponseDTO getProvider(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider", providerId));
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            // Unapproved profiles are not part of the public catalogue.
            throw ResourceNotFoundException.of("Provider", providerId);
        }
        return providerMapper.toDto(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProviderResponseDTO> searchProviders(String city, String area, String keyword,
                                                                Long serviceId, int page, int size) {
        var result = providerRepository.search(
                blankToNull(city), blankToNull(area), blankToNull(keyword), serviceId,
                PageUtils.of(page, size, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "ratingAverage")));
        List<ProviderResponseDTO> content = providerMapper.toDtoList(result.getContent());
        return new PageResponseDTO<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProviderResponseDTO> listProviders(ProviderStatus status, int page, int size) {
        var result = status == null
                ? providerRepository.findAll(PageUtils.of(page, size))
                : providerRepository.findByStatus(status, PageUtils.of(page, size));
        return PageResponseDTO.of(result, providerMapper::toDto);
    }

    @Override
    @Transactional
    public ProviderResponseDTO updateVerification(Long providerId, ProviderStatus status, String note) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider", providerId));

        provider.setStatus(status);
        provider = providerRepository.save(provider);

        String verdict = switch (status) {
            case APPROVED -> "Your Fixora provider profile has been approved. You can now receive bookings.";
            case REJECTED -> "Your Fixora provider profile was rejected." + (note == null ? "" : " Reason: " + note);
            case SUSPENDED -> "Your Fixora provider profile has been suspended." + (note == null ? "" : " Reason: " + note);
            case PENDING -> "Your Fixora provider profile is back under review.";
        };

        notificationService.notifyUser(provider.getUser(), NotificationType.COMPLAINT_STATUS_CHANGED,
                "Provider profile " + status.name().toLowerCase(), verdict, null);

        log.info("Provider {} verification set to {} by admin. Note: {}", providerId, status, note);
        return providerMapper.toDto(provider);
    }

    // ---------------------------------------------------------------- offerings

    @Override
    @Transactional(readOnly = true)
    public List<ProviderServiceResponseDTO> listProviderServices(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider", providerId));
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw ResourceNotFoundException.of("Provider", providerId);
        }
        return providerServiceRepository.findByProviderIdOrderByIdAsc(providerId)
                .stream().filter(ps -> Boolean.TRUE.equals(ps.getActive()))
                .map(providerMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderServiceResponseDTO> listMyServices(Long userId) {
        Provider provider = requireProvider(userId);
        return providerServiceRepository.findByProviderIdOrderByIdAsc(provider.getId())
                .stream().map(providerMapper::toDto).toList();
    }

    @Override
    @Transactional
    public ProviderServiceResponseDTO addMyService(Long userId, ProviderServiceRequestDTO request) {
        Provider provider = requireProvider(userId);

        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> ResourceNotFoundException.of("Service", request.serviceId()));
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BadRequestException("That service is not currently offered on Fixora.");
        }

        if (providerServiceRepository.existsByProviderIdAndServiceId(provider.getId(), service.getId())) {
            throw new DuplicateResourceException("You already offer " + service.getName() + ".");
        }

        ProviderService offering = ProviderService.builder()
                .provider(provider)
                .service(service)
                .customPrice(request.customPrice())
                .active(request.active() == null || request.active())
                .build();

        return providerMapper.toDto(providerServiceRepository.save(offering));
    }

    @Override
    @Transactional
    public void removeMyService(Long userId, Long providerServiceId) {
        Provider provider = requireProvider(userId);
        ProviderService offering = providerServiceRepository.findById(providerServiceId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProviderService", providerServiceId));

        if (!offering.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("That offering belongs to another provider.");
        }
        if (!bookingRepository.findByProviderId(provider.getId()).isEmpty()) {
            // Keep history intact: deactivate instead of deleting when bookings exist.
            offering.setActive(false);
            providerServiceRepository.save(offering);
            return;
        }
        providerServiceRepository.delete(offering);
    }

    // -------------------------------------------------------------- availability

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponseDTO> listAvailability(Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw ResourceNotFoundException.of("Provider", providerId);
        }
        return availabilityRepository.findByProviderIdOrderByDayOfWeekAscStartTimeAsc(providerId)
                .stream().map(providerMapper::toDto).toList();
    }

    @Override
    @Transactional
    public List<AvailabilityResponseDTO> replaceMyAvailability(Long userId, List<AvailabilityRequestDTO> windows) {
        Provider provider = requireProvider(userId);

        for (AvailabilityRequestDTO window : windows) {
            if (window.startTime() == null || window.endTime() == null
                    || !window.endTime().isAfter(window.startTime())) {
                throw new BadRequestException("Each availability window must end after it starts ("
                        + window.dayOfWeek() + ").");
            }
        }

        availabilityRepository.deleteByProviderId(provider.getId());

        List<ProviderAvailability> saved = new ArrayList<>();
        for (AvailabilityRequestDTO window : windows) {
            saved.add(availabilityRepository.save(ProviderAvailability.builder()
                    .provider(provider)
                    .dayOfWeek(window.dayOfWeek())
                    .startTime(window.startTime())
                    .endTime(window.endTime())
                    .active(window.active() == null || window.active())
                    .build()));
        }

        return saved.stream().map(providerMapper::toDto).toList();
    }

    // ------------------------------------------------------------------ earnings

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> earningsSummary(Long userId) {
        Provider provider = requireProvider(userId);

        Map<BookingStatus, Long> byStatus = new LinkedHashMap<>();
        BigDecimal completedValue = BigDecimal.ZERO;
        for (BookingStatus status : BookingStatus.values()) {
            List<Booking> bookings = bookingRepository.findByProviderIdAndStatus(provider.getId(), status);
            byStatus.put(status, (long) bookings.size());
            if (status == BookingStatus.COMPLETED) {
                completedValue = bookings.stream()
                        .map(Booking::getAgreedPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        Double average = reviewRepository.averageRatingForProvider(provider.getId());
        long reviewCount = reviewRepository.countForProvider(provider.getId());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("providerId", provider.getId());
        summary.put("businessName", provider.getBusinessName());
        summary.put("status", provider.getStatus());
        summary.put("bookingsByStatus", byStatus);
        summary.put("completedBookings", byStatus.getOrDefault(BookingStatus.COMPLETED, 0L));
        summary.put("pendingBookings", byStatus.getOrDefault(BookingStatus.PENDING, 0L));
        summary.put("acceptedBookings", byStatus.getOrDefault(BookingStatus.ACCEPTED, 0L));
        summary.put("inProgressBookings", byStatus.getOrDefault(BookingStatus.IN_PROGRESS, 0L));
        summary.put("cancelledBookings", byStatus.getOrDefault(BookingStatus.CANCELLED, 0L));
        summary.put("completedValue", completedValue);
        summary.put("averageRating", average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(2, java.math.RoundingMode.HALF_UP));
        summary.put("reviewCount", reviewCount);
        summary.put("estimateDisclaimer",
                "Earnings are an estimate from completed Fixora bookings in this database. No payment gateway is connected.");
        return summary;
    }

    // ------------------------------------------------------------------ helpers

    private Provider requireProvider(Long userId) {
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedActionException(
                        "This account does not have a provider profile yet."));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
