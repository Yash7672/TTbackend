package com.fixora.service.ai;

import com.fixora.dto.request.GenerateDescriptionRequestDTO;
import com.fixora.dto.response.ComplaintClassificationResponseDTO;
import com.fixora.dto.response.GeneratedDescriptionResponseDTO;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.enums.ComplaintCategory;
import com.fixora.enums.ComplaintPriority;
import com.fixora.enums.PricingType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * The fallback brain.
 *
 * Pure, deterministic text generation from real catalogue rows. No randomness, so
 * the same question and the same data always produce the same answer — which is
 * exactly what makes it testable. It never states a price, rating, provider or
 * slot that was not passed in from the database.
 */
@Component
public class DeterministicAssistant {

    // ------------------------------------------------------------------ chat

    public String chatReply(IntentParser.IntentResult intent,
                            List<Service> services,
                            Map<Long, List<ServicePackage>> packagesByService,
                            String city,
                            String detectedCategory) {

        String faq = faqAnswer(intent.rawMessage());
        if (faq != null && (intent.kind() == IntentParser.IntentKind.FAQ_BOOKING
                || intent.kind() == IntentParser.IntentKind.FAQ_PRICING
                || intent.kind() == IntentParser.IntentKind.CANCELLATION_POLICY
                || intent.kind() == IntentParser.IntentKind.UNKNOWN)) {
            return faq + serviceTail(services, packagesByService, detectedCategory);
        }

        return switch (intent.kind()) {
            case GREETING -> "Hello! I am the Fixora AI Assistant. Tell me what needs fixing — "
                    + "for example \"my bathroom tap is leaking\" or \"AC not cooling\" — and I will point you to the "
                    + "right Fixora service with real packages and prices. "
                    + locationLine(city) + "I can also explain how booking, cancellation and reviews work.";
            case THANKS -> "Happy to help. "
                    + (services.isEmpty()
                        ? "Ask me any time about a service, a package price or how booking works."
                        : "You can open " + services.get(0).getName() + " whenever you are ready to pick a package.");
            case PRICE_QUERY -> priceReply(intent, services, packagesByService, city);
            case BOOKING_HELP -> "Booking on Fixora takes four steps: (1) choose a service, "
                    + "(2) pick a package, date and time, (3) confirm the address and submit the booking, "
                    + "(4) track the status as the provider accepts and completes the work. "
                    + "The price you see is the package price from our catalogue — nothing is added at checkout. "
                    + (services.isEmpty() ? "" : "A good starting point is " + services.get(0).getName() + ".")
                    + locationLine(city);
            case CANCELLATION_POLICY -> cancellationPolicy() + serviceTail(services, packagesByService, detectedCategory);
            case COMPLAINT_HELP -> "I am sorry something went wrong. Open Support from the menu, describe what happened, "
                    + "and attach the booking reference. Complaints are stored with status OPEN and an administrator "
                    + "reviews them in the admin panel. You can also leave a review once the booking is completed. "
                    + "I will never change a booking or a complaint on my own — a person always reviews it.";
            case PROVIDER_INFO -> "To work with Fixora, register with the role Provider. Your profile is created with "
                    + "status PENDING and an administrator reviews it; only APPROVED providers appear in search and can "
                    + "accept bookings. After approval you add the services you offer and your weekly availability.";
            case FIND_SERVICE, UNKNOWN, FAQ_PRICING, FAQ_BOOKING ->
                    findServiceReply(intent, services, packagesByService, city, detectedCategory);
        };
    }

    private String priceReply(IntentParser.IntentResult intent,
                              List<Service> services,
                              Map<Long, List<ServicePackage>> packagesByService,
                              String city) {
        if (services.isEmpty()) {
            String faq = faqAnswer(intent.rawMessage());
            return (faq != null ? faq + " " : "")
                    + "Tell me the service you need — for example \"AC general service price\" — and I will read the "
                    + "exact package prices from the Fixora catalogue." + locationLine(city);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Here are the real Fixora packages");
        sb.append(intent.maxPrice() != null ? " within your budget of Rs." + intent.maxPrice() : "");
        sb.append(":\n");
        int shown = 0;
        for (Service service : services) {
            List<ServicePackage> packages = packagesByService.getOrDefault(service.getId(), List.of());
            if (packages.isEmpty()) {
                continue;
            }
            sb.append("\n- ").append(service.getName()).append(" (")
                    .append(service.getCategory() != null ? service.getCategory().getName() : "Service").append(")\n");
            for (ServicePackage pkg : packages) {
                sb.append("   * ").append(pkg.getName()).append(" - Rs.").append(pkg.getPrice())
                        .append(inrNote(pkg.getPricingType())).append(" (").append(pkg.getDurationMinutes())
                        .append(" min)\n");
                if (pkg.getDescription() != null && !pkg.getDescription().isBlank()) {
                    sb.append("     ").append(pkg.getDescription()).append("\n");
                }
                shown++;
            }
            if (shown >= 6) {
                break;
            }
        }
        sb.append("\nThese prices come straight from the Fixora database and are demo prices for this project.");
        sb.append(" ").append(handoff());
        return sb.toString();
    }

    private String findServiceReply(IntentParser.IntentResult intent,
                                    List<Service> services,
                                    Map<Long, List<ServicePackage>> packagesByService,
                                    String city,
                                    String detectedCategory) {
        if (services.isEmpty()) {
            return "I could not match that to a Fixora service yet. "
                    + "Try naming the job and the item, for example \"tap repair\", \"AC service\", "
                    + "\"sofa cleaning\", \"laptop repair\" or \"haircut at home\". "
                    + "You can also browse the categories on the home page."
                    + locationLine(city);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("I can help you with that. ");
        if (detectedCategory != null) {
            sb.append("This sounds like ").append(detectedCategory).append(". ");
        }
        sb.append("Matching Fixora services:\n");
        for (Service service : services) {
            List<ServicePackage> packages = packagesByService.getOrDefault(service.getId(), List.of());
            sb.append("\n- ").append(service.getName());
            sb.append(" — from Rs.").append(service.getBasePrice());
            sb.append(", about ").append(service.getDurationMinutes()).append(" min");
            if (service.getRatingCount() != null && service.getRatingCount() > 0) {
                sb.append(", rated ").append(service.getRatingAverage()).append("/5 from ")
                        .append(service.getRatingCount()).append(" reviews");
            } else {
                sb.append(", no reviews yet");
            }
            sb.append("\n");
            if (!packages.isEmpty()) {
                sb.append("   Packages: ");
                sb.append(String.join(" | ", packages.stream()
                        .limit(3)
                        .map(p -> p.getName() + " Rs." + p.getPrice())
                        .toList()));
                sb.append("\n");
            }
        }
        sb.append("\n").append(handoff()).append(locationLine(city));
        return sb.toString();
    }

    private String serviceTail(List<Service> services,
                               Map<Long, List<ServicePackage>> packagesByService,
                               String detectedCategory) {
        if (services.isEmpty()) {
            return "";
        }
        Service first = services.get(0);
        StringBuilder sb = new StringBuilder(" ");
        sb.append("If you meant ").append(first.getName()).append(" — packages start at Rs.");
        List<ServicePackage> packages = packagesByService.getOrDefault(first.getId(), List.of());
        sb.append(packages.isEmpty()
                ? String.valueOf(first.getBasePrice())
                : String.valueOf(packages.get(0).getPrice()));
        sb.append(".");
        return sb.toString();
    }

    private String handoff() {
        return "Would you like to see the available service packages? Open the service page and pick a package, "
                + "then choose your address, date and time.";
    }

    private String locationLine(String city) {
        return city == null || city.isBlank()
                ? " Tell me your city and I can also look up providers serving it."
                : " I will look for providers serving " + city + ".";
    }

    private String inrNote(PricingType pricingType) {
        if (pricingType == null) {
            return "";
        }
        return switch (pricingType) {
            case INSPECTION_FEE -> " (inspection fee only — repair work is billed separately)";
            case STARTING_PRICE -> " (starting from)";
            case QUOTATION -> " (final price confirmed after inspection)";
            case FIXED_PRICE -> " (fixed price)";
        };
    }

    // ------------------------------------------------------------------ search

    public String searchMessage(String query, int resultCount, String categoryName, java.math.BigDecimal maxPrice) {
        if (resultCount == 0) {
            return "No Fixora service matches \"" + query + "\" right now. Try broader wording such as "
                    + "\"cleaning\", \"AC\", \"electrician\" or \"laptop repair\".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(resultCount).append(" service").append(resultCount == 1 ? "" : "s");
        if (categoryName != null) {
            sb.append(" in ").append(categoryName);
        }
        if (maxPrice != null) {
            sb.append(" up to Rs.").append(maxPrice);
        }
        sb.append(" for \"").append(query).append("\".");
        return sb.toString();
    }

    // --------------------------------------------------------------- assistance

    public String bookingAssistance(Service service, List<ServicePackage> packages, String message) {
        if (service == null) {
            return "Tell me which service you need and I will explain the package options from the Fixora catalogue.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("For ").append(service.getName()).append(", Fixora offers these packages:\n");
        for (ServicePackage pkg : packages) {
            sb.append("\n- ").append(pkg.getName()).append(" — Rs.").append(pkg.getPrice())
                    .append(inrNote(pkg.getPricingType())).append(", about ")
                    .append(pkg.getDurationMinutes()).append(" min");
            if (pkg.getDescription() != null && !pkg.getDescription().isBlank()) {
                sb.append("\n   ").append(pkg.getDescription());
            }
            if (pkg.getIncludedWork() != null && !pkg.getIncludedWork().isBlank()) {
                sb.append("\n   Included: ").append(pkg.getIncludedWork());
            }
            if (pkg.getExcludedWork() != null && !pkg.getExcludedWork().isBlank()) {
                sb.append("\n   Not included: ").append(pkg.getExcludedWork());
            }
            sb.append("\n");
        }
        sb.append("\nPick the package that matches what you need — for example an inspection package when you only want "
                + "a check-up, and a service package when you want the work carried out. ");
        sb.append("I will not create a booking for you: open the service page, choose the package and confirm the "
                + "address, date and time yourself so you can review everything before submitting.");
        return sb.toString();
    }

    // -------------------------------------------------------------------- faqs

    /** Answers about how Fixora itself works. Returns null when nothing matches. */
    public String faqAnswer(String message) {
        if (message == null) {
            return null;
        }
        String m = message.toLowerCase(Locale.ROOT);

        if (containsAny(m, "how do i book", "how to book", "booking process", "how does booking", "book a service")) {
            return "Book in four steps: choose a service, pick a package, choose an address plus date and time, "
                    + "then confirm. The booking is created with status PENDING and the provider accepts or rejects it. "
                    + "You can follow every change on the booking detail page.";
        }
        if (containsAny(m, "cancel", "cancellation")) {
            return cancellationPolicy();
        }
        if (containsAny(m, "refund", "money back")) {
            return "This version of Fixora does not connect to a payment gateway, so there is nothing to refund "
                    + "automatically. Payment status is recorded on the booking, and refunds would be handled by support. "
                    + "Open Support from the menu if you need help.";
        }
        if (containsAny(m, "price", "pricing", "cost", "charges", "how much")) {
            return "Every service has one or more packages and each package has its own price in the Fixora database. "
                    + "Ask me about a specific service, for example \"AC service price\", and I will list the real "
                    + "packages. Prices shown in this project are demo prices.";
        }
        if (containsAny(m, "payment", "pay", "upi", "card", "cash")) {
            return "Payment is not processed inside Fixora. Each booking stores a payment status field, but no gateway "
                    + "is connected in this version, so payment happens directly between you and the provider.";
        }
        if (containsAny(m, "verified", "verification", "background check")) {
            return "Providers register with status PENDING and an administrator approves them. Only APPROVED providers "
                    + "appear in search and can accept bookings. Fixora shows the real status; it does not claim that "
                    + "every provider has completed a background check.";
        }
        if (containsAny(m, "review", "rating", "feedback")) {
            return "You can review a service only after the booking is COMPLETED, one review per booking, between 1 and "
                    + "5 stars. Averages shown on Fixora are calculated from real reviews only — services with no "
                    + "reviews show \"No reviews yet\".";
        }
        if (containsAny(m, "address", "location", "where")) {
            return "Save your addresses under Profile > Addresses and mark one as default. Addresses belong to your "
                    + "account, and a booking can only use an address that belongs to you.";
        }
        if (containsAny(m, "provider", "partner", "work with you", "join")) {
            return "Register with the Provider role to offer services. Your profile starts as PENDING, an administrator "
                    + "reviews it, and once APPROVED you can add offered services and weekly availability.";
        }
        if (containsAny(m, "safe", "dangerous", "gas leak", "electrical fire", "smoke")) {
            return "If this is an emergency, contact local emergency services first. Fixora can book a technician for "
                    + "repairs and inspections, but the assistant cannot give safe step-by-step instructions for gas, "
                    + "mains electricity or other hazardous work.";
        }
        return null;
    }

    private String cancellationPolicy() {
        return "Cancellation rules on Fixora: a PENDING or ACCEPTED booking can be cancelled by the customer, and the "
                + "cancel button disappears once the booking is IN_PROGRESS, COMPLETED, REJECTED or already CANCELLED. "
                + "A provider can cancel a booking assigned to them before it starts. Every change is stored in the "
                + "booking status history so there is a clear record.";
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------- complaint triage

    /** Rule-based classification. Always advisory — an admin confirms it. */
    public ComplaintClassificationResponseDTO classify(String subject, String description) {
        String text = ((subject == null ? "" : subject) + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);

        ComplaintCategory category;
        if (containsAny(text, "late", "did not arrive", "didn't arrive", "no show", "delayed", "didnt come")) {
            category = ComplaintCategory.LATE_ARRIVAL;
        } else if (containsAny(text, "rude", "behav", "misconduct", "argued", "behaved")) {
            category = ComplaintCategory.PROVIDER_BEHAVIOUR;
        } else if (containsAny(text, "price", "charge", "extra money", "overcharged", "cost", "billing")) {
            category = ComplaintCategory.PRICING;
        } else if (containsAny(text, "cancel", "reschedule", "refund")) {
            category = ComplaintCategory.CANCELLATION;
        } else if (containsAny(text, "poor", "incomplete", "damage", "broke", "quality", "not fixed", "still leaking", "again broke")) {
            category = ComplaintCategory.SERVICE_QUALITY;
        } else {
            category = ComplaintCategory.OTHER;
        }

        ComplaintPriority priority = ComplaintPriority.MEDIUM;
        if (containsAny(text, "damage", "injur", "safety", "shock", "fire", "flood", "unsafe", "harass")) {
            priority = ComplaintPriority.HIGH;
        } else if (containsAny(text, "slight", "minor", "small", "question", "clarif")) {
            priority = ComplaintPriority.LOW;
        }

        String summary = "Customer reports: " + truncate(subject == null ? description : subject, 140)
                + " Classified as " + category.name().replace('_', ' ').toLowerCase(Locale.ROOT)
                + " with suggested priority " + priority.name() + ".";

        return new ComplaintClassificationResponseDTO(
                category,
                priority,
                summary,
                "Automatically suggested by Fixora rules; an administrator makes the final decision.",
                "fallback");
    }

    // ------------------------------------------------------- description drafts

    /** Draft service copy. Always returned as a draft for admin review. */
    public GeneratedDescriptionResponseDTO draftDescription(GenerateDescriptionRequestDTO request) {
        String name = request.serviceName() == null ? "this service" : request.serviceName().trim();
        String category = request.categoryName() == null ? "local services" : request.categoryName().trim();
        String packages = request.packageNames() == null ? "" : request.packageNames().trim();
        String pricing = request.pricingType() == null ? "STARTING_PRICE" : request.pricingType().name();

        String shortDescription = "Professional " + name.toLowerCase(Locale.ROOT)
                + " at your home, booked through Fixora with transparent package pricing.";

        StringBuilder full = new StringBuilder();
        full.append(name).append(" is part of the ").append(category)
                .append(" category on Fixora. A verified booking flow lets you choose a package, pick a date and "
                        + "time, and track the job from request to completion. ")
                .append(pricingNote(pricing))
                .append(" Every booking keeps a status history, and reviews are only accepted from customers whose "
                        + "booking is completed.");
        if (!packages.isBlank()) {
            full.append(" Available packages: ").append(packages).append(".");
        }
        if (request.includedWork() != null && !request.includedWork().isBlank()) {
            full.append(" Included work: ").append(request.includedWork()).append(".");
        }
        if (request.excludedWork() != null && !request.excludedWork().isBlank()) {
            full.append(" Not included: ").append(request.excludedWork()).append(".");
        }

        List<String> faqs = new ArrayList<>(List.of(
                "What is included in " + name + "?",
                "How long does " + name + " usually take?",
                "Can I reschedule or cancel my " + name + " booking?",
                "Do I need to buy spare parts separately?"
        ));

        String packageExplanation = packages.isBlank()
                ? "No packages were provided, so describe each package's price, duration and scope before publishing."
                : "Explain the difference between these packages (" + packages + ") so customers can compare: what each "
                        + "one includes, how long it takes, and whether extra work is billed separately.";

        return new GeneratedDescriptionResponseDTO(
                shortDescription,
                full.toString(),
                faqs,
                packageExplanation,
                true,
                "fallback");
    }

    private String pricingNote(String pricingType) {
        return switch (pricingType) {
            case "INSPECTION_FEE" -> "This service is priced as an inspection fee; repair work after the inspection is "
                    + "quoted and billed separately. ";
            case "QUOTATION" -> "The final price is confirmed by the provider after inspection. ";
            case "FIXED_PRICE" -> "This service has a fixed price per package. ";
            default -> "Prices shown are starting prices; the provider confirms the final amount before work begins. ";
        };
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max - 1) + "…";
    }
}
