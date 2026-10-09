package com.fixora.service;

import com.fixora.BaseDataTest;
import com.fixora.dto.request.BookingRequestDTO;
import com.fixora.dto.response.BookingResponseDTO;
import com.fixora.entity.*;
import com.fixora.enums.BookingStatus;
import com.fixora.enums.PricingType;
import com.fixora.enums.ProviderStatus;
import com.fixora.enums.UserRole;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.BookingConflictException;
import com.fixora.exception.InvalidBookingTransitionException;
import com.fixora.exception.UnauthorizedActionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest extends BaseDataTest {

    @Autowired private BookingService bookingService;

    @Test
    void createsBookingAndUsesTheServerSidePrice() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("AC Services"), "AC General Service", "499", PricingType.STARTING_PRICE);
        ServicePackage pkg = servicePackage(service, "General Service", "499", 60);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        BookingResponseDTO booking = bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(),
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, "Please call before arriving"));

        assertThat(booking.id()).isNotNull();
        assertThat(booking.bookingRef()).startsWith("FX-");
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        // The price came from the package, not from the request.
        assertThat(booking.agreedPrice()).isEqualByComparingTo(new BigDecimal("499"));
        assertThat(booking.packageName()).isEqualTo("General Service");
        assertThat(booking.statusHistory()).hasSize(1);
    }

    @Test
    void rejectsABookingDateInThePast() {
        User customer = customer();
        Service service = service(category("Plumbing"), "Tap Repair", "199", PricingType.STARTING_PRICE);
        ServicePackage pkg = servicePackage(service, "Standard Service", "199", 30);
        Address address = address(customer, true);

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), null, address.getId(), LocalDate.now().minusDays(1), LocalTime.of(10, 0), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    void rejectsAnUnknownPackage() {
        User customer = customer();
        Address address = address(customer, true);

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                999_999L, null, address.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void rejectsAnAddressThatBelongsToSomeoneElse() {
        User customer = customer();
        User other = customer();
        Service service = service(category("Plumbing"), "Tap Installation", "249", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "249", 45);
        Address foreignAddress = address(other, true);

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), null, foreignAddress.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void rejectsAProviderThatIsNotApproved() {
        User customer = customer();
        Provider pending = provider(ProviderStatus.PENDING);
        Service service = service(category("Carpentry"), "Furniture Assembly", "399", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "399", 60);
        makeProviderAvailableFor(pending, service);
        Address address = address(customer, true);

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), pending.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not approved");
    }

    @Test
    void rejectsAProviderThatDoesNotOfferTheService() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service offered = service(category("Painting"), "Room Painting", "3499", PricingType.STARTING_PRICE);
        Service other = service(category("Painting"), "Wall Touch-Up", "999", PricingType.FIXED_PRICE);
        makeProviderAvailableFor(provider, offered);
        ServicePackage pkg = servicePackage(other, "Basic", "999", 120);
        Address address = address(customer, true);

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not offer");
    }

    @Test
    void rejectsATimeOutsideTheProviderAvailability() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Electrician"), "Fan Installation", "249", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "249", 45);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(1),
                LocalTime.of(20, 0), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void rejectsAnOverlappingBookingForTheSameProvider() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Home Cleaning"), "Sofa Cleaning", "499", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "499", 60);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        LocalDate date = LocalDate.now().plusDays(3);
        bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), date, LocalTime.of(10, 0), null, null));

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), date, LocalTime.of(10, 30), null, null)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("overlapping");
    }

    @Test
    void rejectsAnIllegalStatusTransition() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Pest Control"), "Ant Control", "699", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "699", 45);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        BookingResponseDTO booking = bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(11, 0), null, null));

        // PENDING -> COMPLETED is not an allowed transition.
        assertThatThrownBy(() -> bookingService.complete(provider.getUser().getId(), booking.id()))
                .isInstanceOf(InvalidBookingTransitionException.class);
    }

    @Test
    void walksTheHappyPathFromPendingToCompleted() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Appliance Repair"), "Geyser Repair", "399", PricingType.STARTING_PRICE);
        ServicePackage pkg = servicePackage(service, "Standard Service", "399", 60);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        BookingResponseDTO created = bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(2), LocalTime.of(12, 0), null, null));

        Long providerUserId = provider.getUser().getId();
        assertThat(bookingService.accept(providerUserId, created.id()).status()).isEqualTo(BookingStatus.ACCEPTED);
        assertThat(bookingService.start(providerUserId, created.id()).status()).isEqualTo(BookingStatus.IN_PROGRESS);
        BookingResponseDTO completed = bookingService.complete(providerUserId, created.id());

        assertThat(completed.status()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(completed.statusHistory()).hasSize(4);
        assertThat(completed.canReview()).isTrue();
    }

    @Test
    void cancelsABookingAndThenBlocksAcceptance() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Plumbing"), "Drain Cleaning", "449", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "449", 60);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        BookingResponseDTO created = bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(14, 0), null, null));

        BookingResponseDTO cancelled = bookingService.cancel(created.id(), customer.getId(), UserRole.CUSTOMER, "Change of plan");
        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.statusNote()).isEqualTo("Change of plan");

        assertThatThrownBy(() -> bookingService.accept(provider.getUser().getId(), created.id()))
                .isInstanceOf(InvalidBookingTransitionException.class);
    }

    @Test
    void anotherCustomerCannotCancelSomeoneElsesBooking() {
        User customer = customer();
        User stranger = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Vehicle Services"), "Car Wash", "599", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "599", 60);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        BookingResponseDTO created = bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(9, 0), null, null));

        assertThatThrownBy(() -> bookingService.cancel(created.id(), stranger.getId(), UserRole.CUSTOMER, null))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void aProviderCannotAcceptABookingAssignedToSomeoneElse() {
        User customer = customer();
        Provider assigned = provider(ProviderStatus.APPROVED);
        Provider other = provider(ProviderStatus.APPROVED);
        Service service = service(category("Technology Repair"), "Laptop Repair", "699", PricingType.STARTING_PRICE);
        ServicePackage pkg = servicePackage(service, "Standard Service", "699", 90);
        makeProviderAvailableFor(assigned, service);
        makeProviderAvailableFor(other, service);
        Address address = address(customer, true);

        BookingResponseDTO created = bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), assigned.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(13, 0), null, null));

        assertThatThrownBy(() -> bookingService.accept(other.getUser().getId(), created.id()))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void providerQueueOnlyContainsTheProvidersOwnBookings() {
        User customer = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Other Services"), "Gardening", "799", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "799", 90);
        makeProviderAvailableFor(provider, service);
        Address address = address(customer, true);

        bookingService.createBooking(customer.getId(), new BookingRequestDTO(
                pkg.getId(), provider.getId(), address.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, null));

        var queue = bookingService.providerBookings(provider.getUser().getId(), null, 0, 20);
        assertThat(queue.content()).isNotEmpty();
        assertThat(queue.content()).allMatch(b -> provider.getId().equals(b.providerId()));
    }
}
