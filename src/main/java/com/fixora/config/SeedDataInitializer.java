package com.fixora.config;

import com.fixora.entity.*;
import com.fixora.enums.*;
import com.fixora.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Development seed data.
 *
 * IDEMPOTENT: it returns immediately if the categories table already has rows, so
 * restarting the backend never duplicates the catalogue, and it never deletes
 * anything. Disable it completely with FIXORA_SEED_ENABLED=false.
 *
 * All prices are demo prices for this learning project — they are not official
 * prices from any real company.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fixora.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final AddressRepository addressRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationRepository notificationRepository;

    /** category header = "Name|iconName|description"; rows = "name|price|minutes|PRICING_TYPE|featured" */
    private static final Map<String, String[]> CATALOGUE = new LinkedHashMap<>();

    static {
        CATALOGUE.put("Home Cleaning|sparkles|Deep cleaning for homes and apartments", new String[]{
                "Full Home Cleaning|1999|240|FIXED_PRICE|true",
                "Bathroom Deep Cleaning|599|90|FIXED_PRICE|false",
                "Kitchen Deep Cleaning|899|120|FIXED_PRICE|true",
                "Sofa Cleaning|499|60|FIXED_PRICE|false",
                "Carpet Cleaning|699|90|FIXED_PRICE|false",
                "Mattress Cleaning|599|60|FIXED_PRICE|false",
                "Move-In / Move-Out Cleaning|2499|300|FIXED_PRICE|false",
                "Balcony Cleaning|399|45|FIXED_PRICE|false"
        });
        CATALOGUE.put("Electrician|zap|Licensed electricians for wiring, fittings and repairs", new String[]{
                "Fan Installation|249|45|FIXED_PRICE|false",
                "Fan Repair|199|30|STARTING_PRICE|false",
                "Light Installation|179|30|FIXED_PRICE|false",
                "Switch and Socket Repair|149|30|STARTING_PRICE|false",
                "Wiring Inspection|499|60|INSPECTION_FEE|false",
                "MCB and Fuse Repair|299|45|STARTING_PRICE|false",
                "Electrical Troubleshooting|399|60|INSPECTION_FEE|false",
                "Appliance Connection|199|30|FIXED_PRICE|false"
        });
        CATALOGUE.put("Plumbing|droplets|Taps, leaks, fittings and drainage work", new String[]{
                "Tap Repair|199|30|STARTING_PRICE|true",
                "Tap Installation|249|45|FIXED_PRICE|false",
                "Washbasin Installation|399|60|FIXED_PRICE|false",
                "Toilet Repair|349|60|STARTING_PRICE|false",
                "Flush Repair|249|45|STARTING_PRICE|false",
                "Pipe Leakage Repair|399|60|STARTING_PRICE|false",
                "Drain Cleaning|449|60|FIXED_PRICE|false",
                "Bathroom Fitting Installation|899|120|FIXED_PRICE|false"
        });
        CATALOGUE.put("AC Services|wind|Air conditioner service, repair and installation", new String[]{
                "AC Inspection|199|30|INSPECTION_FEE|false",
                "AC General Service|499|60|STARTING_PRICE|true",
                "AC Deep Cleaning|799|90|FIXED_PRICE|false",
                "AC Gas Refill|2499|120|QUOTATION|false",
                "AC Installation|1499|120|STARTING_PRICE|false",
                "AC Uninstallation|799|60|FIXED_PRICE|false",
                "AC Repair|499|60|STARTING_PRICE|false",
                "Cooling Problem Diagnosis|349|45|INSPECTION_FEE|false"
        });
        CATALOGUE.put("Appliance Repair|washing-machine|Washing machine, fridge and appliance repairs", new String[]{
                "Washing Machine Repair|449|60|STARTING_PRICE|true",
                "Refrigerator Repair|499|60|STARTING_PRICE|true",
                "Microwave Repair|399|45|STARTING_PRICE|false",
                "Dishwasher Repair|599|75|STARTING_PRICE|false",
                "Water Purifier Service|599|60|FIXED_PRICE|false",
                "Geyser Repair|399|60|STARTING_PRICE|false",
                "Chimney Cleaning|899|90|FIXED_PRICE|false",
                "Induction Cooktop Repair|349|45|STARTING_PRICE|false"
        });
        CATALOGUE.put("Carpentry|hammer|Furniture assembly, doors and woodwork", new String[]{
                "Furniture Assembly|399|60|FIXED_PRICE|false",
                "Door Repair|349|45|STARTING_PRICE|false",
                "Lock Installation|299|45|FIXED_PRICE|false",
                "Drawer Repair|249|30|FIXED_PRICE|false",
                "Bed Assembly|399|60|FIXED_PRICE|false",
                "Shelf Installation|349|45|FIXED_PRICE|false",
                "Hinge Repair|199|30|FIXED_PRICE|false"
        });
        CATALOGUE.put("Painting|paint-roller|Interior and exterior painting work", new String[]{
                "Room Painting|3499|480|STARTING_PRICE|false",
                "Full Home Painting|14999|1200|QUOTATION|false",
                "Wall Touch-Up|999|120|FIXED_PRICE|false",
                "Texture Painting|5499|600|QUOTATION|false",
                "Waterproofing Assessment|899|90|INSPECTION_FEE|false",
                "Exterior Painting Consultation|999|90|INSPECTION_FEE|false"
        });
        CATALOGUE.put("Pest Control|bug|Treatment for common household pests", new String[]{
                "General Pest Inspection|499|45|INSPECTION_FEE|false",
                "Cockroach Control|899|60|FIXED_PRICE|true",
                "Ant Control|699|45|FIXED_PRICE|false",
                "Mosquito Control|749|45|FIXED_PRICE|false",
                "Termite Inspection|999|60|INSPECTION_FEE|false",
                "Bed Bug Treatment|1499|120|FIXED_PRICE|false",
                "Rodent Control|899|75|FIXED_PRICE|false"
        });
        CATALOGUE.put("Beauty and Personal Care|scissors|Salon and personal care services", new String[]{
                "Women's Haircut|499|45|FIXED_PRICE|false",
                "Hair Styling|699|60|FIXED_PRICE|false",
                "Facial|899|60|FIXED_PRICE|true",
                "Cleanup|599|45|FIXED_PRICE|false",
                "Manicure|449|45|FIXED_PRICE|false",
                "Pedicure|499|45|FIXED_PRICE|false",
                "Waxing|699|60|FIXED_PRICE|false",
                "Men's Grooming|399|45|FIXED_PRICE|false",
                "Bridal Makeup Consultation|1999|120|STARTING_PRICE|false"
        });
        CATALOGUE.put("Salon at Home|wand-sparkles|Salon treatments in your living room", new String[]{
                "Haircut at Home|399|45|FIXED_PRICE|false",
                "Hair Colouring|1499|120|STARTING_PRICE|false",
                "Hair Spa|999|75|FIXED_PRICE|false",
                "Beard Styling|249|30|FIXED_PRICE|false",
                "Grooming Packages|1299|90|STARTING_PRICE|false"
        });
        CATALOGUE.put("Appliance Installation|plug|Installation of TVs and home appliances", new String[]{
                "TV Installation|599|60|FIXED_PRICE|false",
                "Washing Machine Installation|499|45|FIXED_PRICE|false",
                "Water Purifier Installation|699|60|FIXED_PRICE|false",
                "Geyser Installation|599|60|FIXED_PRICE|false",
                "Kitchen Appliance Installation|799|75|FIXED_PRICE|false"
        });
        CATALOGUE.put("Vehicle Services|car|At-home car and bike care", new String[]{
                "Car Wash|599|60|FIXED_PRICE|false",
                "Bike Service|799|90|STARTING_PRICE|false",
                "Car Battery Assistance|499|45|FIXED_PRICE|false",
                "Basic Vehicle Inspection|699|60|INSPECTION_FEE|false",
                "Puncture Assistance|349|30|FIXED_PRICE|false",
                "Interior Car Cleaning|899|90|FIXED_PRICE|false"
        });
        CATALOGUE.put("Technology Repair|laptop|Computers, laptops, phones and networks", new String[]{
                "Computer Repair|599|75|STARTING_PRICE|false",
                "Laptop Repair|699|90|STARTING_PRICE|true",
                "Laptop Hardware Diagnosis|399|45|INSPECTION_FEE|false",
                "Mobile Repair|499|60|STARTING_PRICE|false",
                "Mobile Screen Replacement|1999|90|QUOTATION|false",
                "Printer Repair|549|60|STARTING_PRICE|false",
                "Wi-Fi Troubleshooting|399|45|FIXED_PRICE|false"
        });
        CATALOGUE.put("Other Services|wrench|Shifting, gardening and general handyman help", new String[]{
                "Home Shifting Assistance|4999|480|QUOTATION|false",
                "Furniture Moving|1499|120|STARTING_PRICE|false",
                "Gardening|799|90|FIXED_PRICE|false",
                "General Handyman Services|399|60|STARTING_PRICE|false"
        });
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existing = categoryRepository.count();
        if (existing > 0) {
            log.info("Seed data skipped: {} categories already present in the database.", existing);
            return;
        }

        log.info("Seeding Fixora demo data (first run)...");

        Map<String, Service> servicesByName = seedCatalogue();
        Map<String, List<ServicePackage>> packagesByService = seedPackages(servicesByName);
        SeededUsers users = seedUsers();
        seedProviderOfferings(servicesByName, users);
        List<Address> addresses = seedAddresses(users.customer());
        seedBookings(servicesByName, packagesByService, users, addresses);
        seedComplaintAndNotifications(users);

        log.info("Fixora seed complete: {} categories, {} services, {} packages, {} providers.",
                categoryRepository.count(), serviceRepository.count(),
                packageRepository.count(), providerRepository.count());
        log.info("Development logins -> admin@fixora.local / Admin@123, provider@fixora.local / Provider@123, "
                + "customer@fixora.local / Customer@123  (DEVELOPMENT ONLY)");
    }

    // ------------------------------------------------------------- catalogue

    private Map<String, Service> seedCatalogue() {
        Map<String, Service> services = new LinkedHashMap<>();
        int categoryOrder = 0;

        for (Map.Entry<String, String[]> entry : CATALOGUE.entrySet()) {
            String[] header = entry.getKey().split("\\|");
            String categoryName = header[0];
            String icon = header.length > 1 ? header[1] : "sparkles";
            String description = header.length > 2 ? header[2] : null;
            String categorySlug = slug(categoryName);

            Category category = categoryRepository.save(Category.builder()
                    .name(categoryName)
                    .slug(categorySlug)
                    .description(description)
                    .iconName(icon)
                    .imageUrl("/images/categories/" + categorySlug + ".svg")
                    .active(true)
                    .displayOrder(categoryOrder++)
                    .build());

            for (String row : entry.getValue()) {
                String[] parts = row.split("\\|");
                String name = parts[0];
                String serviceSlug = slug(name);

                Service service = serviceRepository.save(Service.builder()
                        .name(name)
                        .slug(serviceSlug)
                        .shortDescription(shortCopy(name, categoryName))
                        .description(longCopy(name, categoryName))
                        .category(category)
                        .imageUrl("/images/services/" + serviceSlug + ".svg")
                        .basePrice(new BigDecimal(parts[1]))
                        .pricingType(PricingType.valueOf(parts[3]))
                        .durationMinutes(Integer.parseInt(parts[2]))
                        .active(true)
                        .featured(parts.length > 4 && Boolean.parseBoolean(parts[4]))
                        .ratingAverage(BigDecimal.ZERO)
                        .ratingCount(0)
                        .build());

                services.put(name, service);
            }
        }
        return services;
    }

    /** Package templates per pricing model, so every service has real choices. */
    private Map<String, List<ServicePackage>> seedPackages(Map<String, Service> services) {
        Map<String, List<ServicePackage>> byService = new LinkedHashMap<>();

        for (Service service : services.values()) {
            List<ServicePackage> packages = new ArrayList<>();
            BigDecimal base = service.getBasePrice();
            int baseDuration = service.getDurationMinutes();

            switch (service.getPricingType()) {
                case INSPECTION_FEE -> {
                    packages.add(build(service, "Inspection Visit", base, baseDuration, PricingType.INSPECTION_FEE, 0,
                            "A technician visits, inspects the problem and explains what the fix needs.",
                            "Site visit, inspection and an on-the-spot estimate",
                            "Parts and repair work, which are billed separately after approval"));
                    packages.add(build(service, "Visit + Repair (quoted)", nicen(base.multiply(new BigDecimal("2.5"))),
                            baseDuration + 45, PricingType.QUOTATION, 1,
                            "Inspection followed by the repair, at a price confirmed after the inspection.",
                            "Inspection and the repair work",
                            "Spare parts priced above the approved quotation"));
                }
                case QUOTATION -> {
                    packages.add(build(service, "Site Visit (inspection)", nicen(base.multiply(new BigDecimal("0.4"))),
                            baseDuration / 3, PricingType.INSPECTION_FEE, 0,
                            "A paid visit to assess the work and prepare an exact quotation.",
                            "Assessment, measurements and a written estimate",
                            "The actual work, which is booked after the quotation is accepted"));
                    packages.add(build(service, "Quotation Based Work", base, baseDuration, PricingType.QUOTATION, 1,
                            "The full job, priced after inspection because the scope varies by site.",
                            "All labour for the agreed scope",
                            "Materials and parts, confirmed before the work starts"));
                }
                case STARTING_PRICE -> {
                    packages.add(build(service, "Basic Inspection", nicen(base.multiply(new BigDecimal("0.4"))),
                            Math.max(30, baseDuration / 2), PricingType.INSPECTION_FEE, 0,
                            "A check-up package: the technician diagnoses the problem; any repair is billed separately.",
                            "Inspection, diagnosis and an estimate",
                            "Repair work and parts"));
                    packages.add(build(service, "Standard Service", base, baseDuration, PricingType.STARTING_PRICE, 1,
                            "The usual Fixora service for this job. The final amount is confirmed by the provider before work begins.",
                            "Labour for the standard scope of this service",
                            "Parts, and any extra work beyond the standard scope"));
                    packages.add(build(service, "Complete Service", nicen(base.multiply(new BigDecimal("1.6"))),
                            baseDuration + 30, PricingType.STARTING_PRICE, 2,
                            "A wider scope covering the common extras customers usually need.",
                            "Labour for the extended scope plus a follow-up check",
                            "Parts, and specialist work such as gas refills or major replacements"));
                }
                case FIXED_PRICE -> {
                    packages.add(build(service, "Basic", base, baseDuration, PricingType.FIXED_PRICE, 0,
                            "The standard Fixora package at a fixed price for a single job.",
                            "Labour for one unit or one area",
                            "Extra units, areas or parts"));
                    packages.add(build(service, "Standard", nicen(base.multiply(new BigDecimal("1.35"))),
                            baseDuration + 30, PricingType.FIXED_PRICE, 1,
                            "Covers a typical home requirement with more time on site.",
                            "Labour for up to three units or areas",
                            "Parts and additional areas"));
                    packages.add(build(service, "Premium", nicen(base.multiply(new BigDecimal("1.8"))),
                            baseDuration + 60, PricingType.FIXED_PRICE, 2,
                            "The most thorough package, for larger homes or heavy build-up.",
                            "Labour for the whole home plus a final quality check",
                            "Parts and structural repairs"));
                }
            }

            byService.put(service.getName(), packages);
        }
        return byService;
    }

    private ServicePackage build(Service service, String name, BigDecimal price, int minutes,
                                 PricingType pricingType, int order, String description,
                                 String included, String excluded) {
        return packageRepository.save(ServicePackage.builder()
                .service(service)
                .name(name)
                .description(description)
                .price(price)
                .durationMinutes(Math.max(15, minutes))
                .includedWork(included)
                .excludedWork(excluded)
                .pricingType(pricingType)
                .active(true)
                .displayOrder(order)
                .build());
    }

    // ----------------------------------------------------------------- users

    private record SeededUsers(User customer, User admin, Provider providerOne, Provider providerTwo,
                               Provider providerThree, Provider providerFour) {
    }

    private SeededUsers seedUsers() {
        User admin = saveUser("Fixora Administrator", "admin@fixora.local", "Admin@123", UserRole.ADMIN, "9000000001");
        User customerUser = saveUser("Yashwanth Kumar", "customer@fixora.local", "Customer@123", UserRole.CUSTOMER, "9000000002");
        customerProfileRepository.save(CustomerProfile.builder()
                .user(customerUser)
                .defaultCity("Hyderabad")
                .build());

        Provider one = saveProvider("Hyderabad Home Experts", "provider@fixora.local",
                "Home cleaning and AC specialists serving Kondapur, Gachibowli and Madhapur.", "Hyderabad",
                "Kondapur", 9);
        Provider two = saveProvider("Deccan Appliance Care", "provider2@fixora.local",
                "Appliance and electronics repair technicians with 6+ years of workshop experience.", "Hyderabad",
                "Kukatpally", 6);
        Provider three = saveProvider("Charminar Cleaning Co", "provider3@fixora.local",
                "Pest control, plumbing and painting crew covering the old city and central Hyderabad.", "Hyderabad",
                "Charminar", 11);
        Provider four = saveProvider("Zenith Beauty & Auto Care", "provider4@fixora.local",
                "At-home salon treatments plus car and bike care in one team.", "Hyderabad", "Banjara Hills", 4);

        return new SeededUsers(customerUser, admin, one, two, three, four);
    }

    private User saveUser(String name, String email, String password, UserRole role, String phone) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password(password)      // plain text: DEVELOPMENT ONLY
                .phone(phone)
                .role(role)
                .active(true)
                .build());
    }

    private Provider saveProvider(String businessName, String email, String bio, String city, String area, int years) {
        User user = saveUser(businessName, email, "Provider@123", UserRole.PROVIDER, "9000000003");
        Provider provider = providerRepository.save(Provider.builder()
                .user(user)
                .businessName(businessName)
                .profileImageUrl("/images/providers/" + slug(businessName) + ".svg")
                .bio(bio)
                .city(city)
                .area(area)
                .experienceYears(years)
                .status(ProviderStatus.APPROVED)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .build());
        user.setProvider(provider);
        userRepository.save(user);
        return provider;
    }

    private void seedProviderOfferings(Map<String, Service> services, SeededUsers users) {
        Map<String, List<String>> plan = new LinkedHashMap<>();
        plan.put("Hyderabad Home Experts", List.of(
                "Home Cleaning", "AC Services", "Appliance Installation"));
        plan.put("Deccan Appliance Care", List.of(
                "Appliance Repair", "Technology Repair", "Electrician"));
        plan.put("Charminar Cleaning Co", List.of(
                "Pest Control", "Plumbing", "Painting", "Carpentry"));
        plan.put("Zenith Beauty & Auto Care", List.of(
                "Beauty and Personal Care", "Salon at Home", "Vehicle Services", "Other Services"));

        Map<String, Provider> providers = new LinkedHashMap<>();
        providers.put("Hyderabad Home Experts", users.providerOne());
        providers.put("Deccan Appliance Care", users.providerTwo());
        providers.put("Charminar Cleaning Co", users.providerThree());
        providers.put("Zenith Beauty & Auto Care", users.providerFour());

        Set<String> offered = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : plan.entrySet()) {
            Provider provider = providers.get(entry.getKey());
            for (Service service : services.values()) {
                if (!entry.getValue().contains(service.getCategory().getName())) {
                    continue;
                }
                providerServiceRepository.save(ProviderService.builder()
                        .provider(provider)
                        .service(service)
                        .active(true)
                        .build());
                offered.add(service.getName());
            }
            // Monday to Saturday, 09:00 - 18:00
            for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                availabilityRepository.save(ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(day)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .active(true)
                        .build());
            }
        }
        log.info("Seeded {} provider-service offerings for {} services.", offered.size(), services.size());
    }

    private List<Address> seedAddresses(User customer) {
        Address home = addressRepository.save(Address.builder()
                .customer(customer)
                .label("Home")
                .line1("Flat 402, Sai Residency")
                .line2("12th Main Road, Kondapur")
                .city("Hyderabad")
                .area("Kondapur")
                .pincode("500084")
                .addressType(AddressType.HOME)
                .defaultAddress(true)
                .build());

        Address work = addressRepository.save(Address.builder()
                .customer(customer)
                .label("Work")
                .line1("3rd Floor, Cyber Towers")
                .line2("HITEC City, Madhapur")
                .city("Hyderabad")
                .area("Madhapur")
                .pincode("500081")
                .addressType(AddressType.WORK)
                .defaultAddress(false)
                .build());

        return List.of(home, work);
    }

    // -------------------------------------------------------------- bookings

    private void seedBookings(Map<String, Service> services,
                              Map<String, List<ServicePackage>> packages,
                              SeededUsers users,
                              List<Address> addresses) {
        LocalDate today = LocalDate.now();

        // 1) Completed booking + review → proves the review flow with real data.
        Booking completed = createBooking(services, packages, users.customer(), users.providerOne(),
                "AC General Service", 1, addresses.get(0), today.minusDays(7),
                LocalTime.of(10, 0), BookingStatus.COMPLETED,
                "Please service both indoor and outdoor units.", users);

        reviewRepository.save(Review.builder()
                .booking(completed)
                .customer(users.customer())
                .provider(users.providerOne())
                .service(completed.getService())
                .rating(5)
                .comment("Technician arrived on time, cleaned both units and explained the maintenance tips. "
                        + "Cooling is noticeably better.")
                .visible(true)
                .build());

        // Recompute the aggregates exactly like the live review flow does.
        recalculate(completed.getService(), users.providerOne());

        // 2) Pending booking waiting for the provider.
        createBooking(services, packages, users.customer(), users.providerThree(),
                "Tap Repair", 1, addresses.get(0), today.plusDays(2),
                LocalTime.of(11, 0), BookingStatus.PENDING,
                "Kitchen tap drips even when closed.", users);

        // 3) Accepted booking that is coming up.
        createBooking(services, packages, users.customer(), users.providerTwo(),
                "Washing Machine Repair", 1, addresses.get(1), today.plusDays(5),
                LocalTime.of(15, 0), BookingStatus.ACCEPTED,
                "Front loader stops mid-cycle with an error code.", users);

        // 4) Cancelled booking with a reason in its history.
        Booking cancelled = createBooking(services, packages, users.customer(), users.providerOne(),
                "Sofa Cleaning", 0, addresses.get(0), today.minusDays(3),
                LocalTime.of(17, 0), BookingStatus.CANCELLED,
                null, users);
        cancelled.setStatusNote("Customer rescheduled the visit.");
        bookingRepository.save(cancelled);
        cancelled.addHistory(BookingStatusHistory.builder()
                .status(BookingStatus.CANCELLED)
                .note("Customer rescheduled the visit.")
                .build());
        bookingRepository.save(cancelled);
    }

    private Booking createBooking(Map<String, Service> services,
                                  Map<String, List<ServicePackage>> packages,
                                  User customer,
                                  Provider provider,
                                  String serviceName,
                                  int packageIndex,
                                  Address address,
                                  LocalDate date,
                                  LocalTime start,
                                  BookingStatus status,
                                  String notes,
                                  SeededUsers users) {

        Service service = services.get(serviceName);
        List<ServicePackage> packageList = packages.get(serviceName);
        ServicePackage pkg = packageList.get(Math.min(packageIndex, packageList.size() - 1));

        Booking booking = Booking.builder()
                .bookingRef(reference())
                .customer(customer)
                .provider(provider)
                .service(service)
                .servicePackage(pkg)
                .packageNameSnapshot(pkg.getName())
                .agreedPrice(pkg.getPrice())
                .address(address)
                .bookingDate(date)
                .startTime(start)
                .endTime(start.plusMinutes(pkg.getDurationMinutes()))
                .customerNotes(notes)
                .status(BookingStatus.PENDING)
                .paymentStatus(status == BookingStatus.COMPLETED ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .build();

        booking.addHistory(BookingStatusHistory.builder()
                .status(BookingStatus.PENDING)
                .note("Booking created with " + provider.getBusinessName() + ".")
                .build());

        if (status != BookingStatus.PENDING) {
            booking.addHistory(BookingStatusHistory.builder()
                    .status(status == BookingStatus.CANCELLED ? BookingStatus.ACCEPTED : status)
                    .note(status == BookingStatus.CANCELLED
                            ? "Provider accepted the booking."
                            : "Status advanced during seeding.")
                    .build());
        }
        if (status == BookingStatus.CANCELLED) {
            booking.setStatus(BookingStatus.CANCELLED);
        } else {
            booking.setStatus(status);
        }

        return bookingRepository.save(booking);
    }

    private void recalculate(Service service, Provider provider) {
        Double serviceAvg = reviewRepository.averageRatingForService(service.getId());
        long serviceCount = reviewRepository.countForService(service.getId());
        service.setRatingAverage(serviceAvg == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(serviceAvg).setScale(2, RoundingMode.HALF_UP));
        service.setRatingCount((int) serviceCount);
        serviceRepository.save(service);

        Double providerAvg = reviewRepository.averageRatingForProvider(provider.getId());
        long providerCount = reviewRepository.countForProvider(provider.getId());
        provider.setRatingAverage(providerAvg == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(providerAvg).setScale(2, RoundingMode.HALF_UP));
        provider.setRatingCount((int) providerCount);
        providerRepository.save(provider);
    }

    // ------------------------------------------------- complaints / notifications

    private void seedComplaintAndNotifications(SeededUsers users) {
        Booking latest = bookingRepository.findByCustomerIdOrderByBookingDateDescStartTimeDesc(
                users.customer().getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);

        complaintRepository.save(Complaint.builder()
                .customer(users.customer())
                .booking(latest)
                .subject("Example complaint: service started late")
                .description("This is sample data for the admin complaint queue. It shows how a customer complaint "
                        + "is stored with an OPEN status and a suggested category.")
                .category(ComplaintCategory.LATE_ARRIVAL)
                .status(ComplaintStatus.OPEN)
                .priority(ComplaintPriority.MEDIUM)
                .aiSuggestedCategory(ComplaintCategory.LATE_ARRIVAL.name())
                .aiSuggestedSummary("Sample seeded complaint used to demonstrate the admin triage screen.")
                .build());

        if (latest != null) {
            notificationRepository.save(Notification.builder()
                    .user(users.customer())
                    .type(NotificationType.BOOKING_CREATED)
                    .title("Booking " + latest.getBookingRef() + " created")
                    .message("Your booking was created and is waiting for the provider to respond.")
                    .readFlag(false)
                    .relatedBookingId(latest.getId())
                    .build());
        }

        notificationRepository.save(Notification.builder()
                .user(users.providerOne().getUser())
                .type(NotificationType.BOOKING_CREATED)
                .title("Welcome to Fixora")
                .message("Your provider profile is approved. Add your services and availability to start receiving bookings.")
                .readFlag(false)
                .build());
    }

    // ----------------------------------------------------------------- helpers

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replace("/", " ")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    private String shortCopy(String name, String categoryName) {
        return name + " at home, booked through Fixora with clear package pricing.";
    }

    private String longCopy(String name, String categoryName) {
        return name + " is part of the " + categoryName + " category on Fixora. Choose a package, pick a date and "
                + "time, and track the job from request to completion. Prices shown are demo prices for this project.";
    }

    private BigDecimal nicen(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP)
                .divide(new BigDecimal("10"), 0, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("10"));
    }

    private String reference() {
        return "FX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
