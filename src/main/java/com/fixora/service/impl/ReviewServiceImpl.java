package com.fixora.service.impl;

import com.fixora.dto.request.ReviewRequestDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.dto.response.RatingSummaryDTO;
import com.fixora.dto.response.ReviewResponseDTO;
import com.fixora.entity.*;
import com.fixora.enums.BookingStatus;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.DuplicateResourceException;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.exception.UnauthorizedActionException;
import com.fixora.mapper.ReviewMapper;
import com.fixora.repository.*;
import com.fixora.service.ReviewService;
import com.fixora.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final ProviderRepository providerRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponseDTO createReview(Long customerId, ReviewRequestDTO request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", request.bookingId()));

        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedActionException("You can only review your own bookings.");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only a completed booking can be reviewed.");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new DuplicateResourceException("This booking already has a review.");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5.");
        }

        Review review = reviewRepository.save(Review.builder()
                .booking(booking)
                .customer(booking.getCustomer())
                .provider(booking.getProvider())     // always the real provider of the booking
                .service(booking.getService())
                .rating(request.rating())
                .comment(request.comment())
                .visible(true)
                .build());

        recalculateServiceRating(booking.getService());
        if (booking.getProvider() != null) {
            recalculateProviderRating(booking.getProvider());
        }

        log.info("Review {} created for booking {}", review.getId(), booking.getBookingRef());
        return reviewMapper.toDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReviewResponseDTO> reviewsForService(Long serviceId, int page, int size) {
        if (!serviceRepository.existsById(serviceId)) {
            throw ResourceNotFoundException.of("Service", serviceId);
        }
        return PageResponseDTO.of(
                reviewRepository.findByServiceIdAndVisibleTrueOrderByCreatedAtDesc(serviceId, PageUtils.of(page, size)),
                reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReviewResponseDTO> reviewsForProvider(Long providerId, int page, int size) {
        if (!providerRepository.existsById(providerId)) {
            throw ResourceNotFoundException.of("Provider", providerId);
        }
        return PageResponseDTO.of(
                reviewRepository.findByProviderIdAndVisibleTrueOrderByCreatedAtDesc(providerId, PageUtils.of(page, size)),
                reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryDTO serviceSummary(Long serviceId) {
        return reviewMapper.toSummary(
                reviewRepository.averageRatingForService(serviceId),
                reviewRepository.countForService(serviceId));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryDTO providerSummary(Long providerId) {
        return reviewMapper.toSummary(
                reviewRepository.averageRatingForProvider(providerId),
                reviewRepository.countForProvider(providerId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReviewResponseDTO> allReviews(int page, int size) {
        return PageResponseDTO.of(
                reviewRepository.findAllByOrderByCreatedAtDesc(PageUtils.of(page, size)),
                reviewMapper::toDto);
    }

    @Override
    @Transactional
    public ReviewResponseDTO setVisibility(Long reviewId, boolean visible) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
        review.setVisible(visible);
        review = reviewRepository.save(review);

        recalculateServiceRating(review.getService());
        if (review.getProvider() != null) {
            recalculateProviderRating(review.getProvider());
        }
        return reviewMapper.toDto(review);
    }

    // ------------------------------------------------------------- recomputation

    /** Aggregates are always recomputed from real rows — never estimated. */
    private void recalculateServiceRating(Service service) {
        Double average = reviewRepository.averageRatingForService(service.getId());
        long count = reviewRepository.countForService(service.getId());
        service.setRatingAverage(average == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        service.setRatingCount((int) count);
        serviceRepository.save(service);
    }

    private void recalculateProviderRating(Provider provider) {
        Double average = reviewRepository.averageRatingForProvider(provider.getId());
        long count = reviewRepository.countForProvider(provider.getId());
        provider.setRatingAverage(average == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        provider.setRatingCount((int) count);
        providerRepository.save(provider);
    }
}
