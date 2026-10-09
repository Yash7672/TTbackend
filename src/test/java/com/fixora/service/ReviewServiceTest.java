package com.fixora.service;

import com.fixora.BaseDataTest;
import com.fixora.dto.request.ReviewRequestDTO;
import com.fixora.dto.response.ReviewResponseDTO;
import com.fixora.entity.*;
import com.fixora.enums.BookingStatus;
import com.fixora.enums.PricingType;
import com.fixora.enums.ProviderStatus;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.DuplicateResourceException;
import com.fixora.exception.UnauthorizedActionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewServiceTest extends BaseDataTest {

    @Autowired private ReviewService reviewService;

    private record Fixture(User customer, Provider provider, Service service, ServicePackage pkg, Address address) {
    }

    private Fixture fixtureWithCompletedBooking() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Home Cleaning"), "Carpet Cleaning", "699", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "699", 90);
        Address address = address(customer, true);
        completedBooking(customer, provider, service, pkg, address);
        return new Fixture(customer, provider, service, pkg, address);
    }

    @Test
    void aReviewIsOnlyAcceptedForACompletedBooking() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Painting"), "Wall Touch-Up", "999", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "999", 120);
        Address address = address(customer, true);

        Booking pending = Booking.builder()
                .bookingRef("FX-PENDING1")
                .customer(customer)
                .provider(provider)
                .service(service)
                .servicePackage(pkg)
                .packageNameSnapshot(pkg.getName())
                .agreedPrice(pkg.getPrice())
                .address(address)
                .bookingDate(java.time.LocalDate.now().plusDays(1))
                .startTime(java.time.LocalTime.of(10, 0))
                .endTime(java.time.LocalTime.of(11, 0))
                .status(BookingStatus.PENDING)
                .paymentStatus(com.fixora.enums.PaymentStatus.PENDING)
                .build();
        pending = bookingRepository.save(pending);

        Long pendingId = pending.getId();
        assertThatThrownBy(() -> reviewService.createReview(customer.getId(),
                new ReviewRequestDTO(pendingId, 5, "Great")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void onlyTheBookingOwnerCanReview() {
        Fixture fixture = fixtureWithCompletedBooking();
        User stranger = customer();
        Booking booking = bookingRepository.findByCustomerIdOrderByBookingDateDescStartTimeDesc(
                fixture.customer().getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().get(0);

        Long bookingId = booking.getId();
        assertThatThrownBy(() -> reviewService.createReview(stranger.getId(),
                new ReviewRequestDTO(bookingId, 4, "Not mine")))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void createsReviewAndRecomputesRealAggregates() {
        Fixture fixture = fixtureWithCompletedBooking();
        Booking booking = bookingRepository.findByCustomerIdOrderByBookingDateDescStartTimeDesc(
                fixture.customer().getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().get(0);

        ReviewResponseDTO review = reviewService.createReview(fixture.customer().getId(),
                new ReviewRequestDTO(booking.getId(), 4, "Good work overall."));

        assertThat(review.id()).isNotNull();
        assertThat(review.rating()).isEqualTo(4);
        assertThat(review.providerId()).isEqualTo(fixture.provider().getId());

        var serviceSummary = reviewService.serviceSummary(fixture.service().getId());
        assertThat(serviceSummary.hasReviews()).isTrue();
        assertThat(serviceSummary.reviewCount()).isEqualTo(1);
        assertThat(serviceSummary.averageRating()).isEqualByComparingTo(new BigDecimal("4.00"));

        var providerSummary = reviewService.providerSummary(fixture.provider().getId());
        assertThat(providerSummary.averageRating()).isEqualByComparingTo(new BigDecimal("4.00"));

        Service reloaded = serviceRepository.findById(fixture.service().getId()).orElseThrow();
        assertThat(reloaded.getRatingCount()).isEqualTo(1);
    }

    @Test
    void duplicateReviewsForTheSameBookingAreRejected() {
        Fixture fixture = fixtureWithCompletedBooking();
        Booking booking = bookingRepository.findByCustomerIdOrderByBookingDateDescStartTimeDesc(
                fixture.customer().getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().get(0);

        reviewService.createReview(fixture.customer().getId(), new ReviewRequestDTO(booking.getId(), 5, "First"));
        Long bookingId = booking.getId();

        assertThatThrownBy(() -> reviewService.createReview(fixture.customer().getId(),
                new ReviewRequestDTO(bookingId, 3, "Second")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void aServiceWithNoReviewsReportsNoRatings() {
        Service service = service(category("Electrician"), "Hinge Repair", "199", PricingType.FIXED_PRICE);

        var summary = reviewService.serviceSummary(service.getId());

        assertThat(summary.hasReviews()).isFalse();
        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.averageRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void hidingAReviewRemovesItFromTheAggregates() {
        Fixture fixture = fixtureWithCompletedBooking();
        Booking booking = bookingRepository.findByCustomerIdOrderByBookingDateDescStartTimeDesc(
                fixture.customer().getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().get(0);
        ReviewResponseDTO review = reviewService.createReview(fixture.customer().getId(),
                new ReviewRequestDTO(booking.getId(), 2, "Not great."));

        reviewService.setVisibility(review.id(), false);

        var summary = reviewService.serviceSummary(fixture.service().getId());
        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.hasReviews()).isFalse();
    }
}
