package com.fixora;

import com.fixora.entity.*;
import com.fixora.enums.*;
import com.fixora.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Base class for service-layer tests.
 *
 * Runs against an in-memory H2 database in MySQL compatibility mode, so the real
 * JPA queries (including the search and overlap queries) are exercised — MySQL is
 * still the runtime database, configured through SPRING_DATASOURCE_* in Compose.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseDataTest {

    @Autowired protected UserRepository userRepository;
    @Autowired protected CustomerProfileRepository customerProfileRepository;
    @Autowired protected ProviderRepository providerRepository;
    @Autowired protected ProviderServiceRepository providerServiceRepository;
    @Autowired protected ProviderAvailabilityRepository providerAvailabilityRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected ServiceRepository serviceRepository;
    @Autowired protected ServicePackageRepository packageRepository;
    @Autowired protected AddressRepository addressRepository;
    @Autowired protected BookingRepository bookingRepository;
    @Autowired protected ReviewRepository reviewRepository;
    @Autowired protected ComplaintRepository complaintRepository;
    @Autowired protected NotificationRepository notificationRepository;

    protected String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected User customer() {
        String token = unique("customer");
        User user = userRepository.save(User.builder()
                .fullName("Test Customer")
                .email(token + "@fixora.test")
                .password("Customer@123")
                .phone("9000000000")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build());
        customerProfileRepository.save(CustomerProfile.builder().user(user).defaultCity("Hyderabad").build());
        return user;
    }

    protected User admin() {
        return userRepository.save(User.builder()
                .fullName("Test Admin")
                .email(unique("admin") + "@fixora.test")
                .password("Admin@123")
                .role(UserRole.ADMIN)
                .active(true)
                .build());
    }

    protected Provider provider(ProviderStatus status) {
        User user = userRepository.save(User.builder()
                .fullName("Test Provider")
                .email(unique("provider") + "@fixora.test")
                .password("Provider@123")
                .role(UserRole.PROVIDER)
                .active(true)
                .build());

        Provider provider = providerRepository.save(Provider.builder()
                .user(user)
                .businessName(unique("Fixora Pros"))
                .bio("Test provider")
                .city("Hyderabad")
                .area("Kondapur")
                .experienceYears(5)
                .status(status)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .build());

        user.setProvider(provider);
        userRepository.save(user);
        return provider;
    }

    protected Category category(String name) {
        return categoryRepository.save(Category.builder()
                .name(name + " " + unique("cat"))
                .slug(unique("cat-slug"))
                .description("Test category")
                .iconName("sparkles")
                .active(true)
                .displayOrder(1)
                .build());
    }

    protected Service service(Category category, String name, String price, PricingType pricingType) {
        return serviceRepository.save(Service.builder()
                .name(name)
                .slug(unique("svc-slug"))
                .shortDescription(name + " for tests")
                .description("Test description")
                .category(category)
                .basePrice(new BigDecimal(price))
                .pricingType(pricingType)
                .durationMinutes(60)
                .active(true)
                .featured(false)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .build());
    }

    protected ServicePackage servicePackage(Service service, String name, String price, int minutes) {
        return packageRepository.save(ServicePackage.builder()
                .service(service)
                .name(name)
                .description(name + " package")
                .price(new BigDecimal(price))
                .durationMinutes(minutes)
                .includedWork("Labour")
                .excludedWork("Parts")
                .pricingType(service.getPricingType())
                .active(true)
                .displayOrder(0)
                .build());
    }

    protected Address address(User owner, boolean isDefault) {
        return addressRepository.save(Address.builder()
                .customer(owner)
                .label("Home")
                .line1("Flat 402, Sai Residency")
                .line2("12th Main Road")
                .city("Hyderabad")
                .area("Kondapur")
                .pincode("500084")
                .addressType(AddressType.HOME)
                .defaultAddress(isDefault)
                .build());
    }

    /** Makes the provider offer the service and be free Monday-Sunday 09:00-18:00. */
    protected void makeProviderAvailableFor(Provider provider, Service service) {
        providerServiceRepository.save(ProviderService.builder()
                .provider(provider)
                .service(service)
                .active(true)
                .build());

        for (DayOfWeek day : DayOfWeek.values()) {
            providerAvailabilityRepository.save(ProviderAvailability.builder()
                    .provider(provider)
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .active(true)
                    .build());
        }
    }

    protected Booking completedBooking(User customer, Provider provider, Service service, ServicePackage pkg, Address address) {
        Booking booking = Booking.builder()
                .bookingRef("FX-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .customer(customer)
                .provider(provider)
                .service(service)
                .servicePackage(pkg)
                .packageNameSnapshot(pkg.getName())
                .agreedPrice(pkg.getPrice())
                .address(address)
                .bookingDate(java.time.LocalDate.now().minusDays(3))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .build();
        booking.addHistory(BookingStatusHistory.builder().status(BookingStatus.PENDING).note("created").build());
        booking.addHistory(BookingStatusHistory.builder().status(BookingStatus.COMPLETED).note("done").build());
        return bookingRepository.save(booking);
    }
}
