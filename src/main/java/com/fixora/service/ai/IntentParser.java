package com.fixora.service.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns free text into structured criteria using rules only.
 *
 * This runs for EVERY request, before any language model is consulted. It is what
 * makes fallback mode genuinely useful instead of a canned apology. The synonym
 * table maps how people actually speak onto tokens that appear in the real
 * catalogue, and every candidate is then verified against the database.
 */
@Component
public class IntentParser {

    public enum IntentKind {
        FIND_SERVICE,
        PRICE_QUERY,
        BOOKING_HELP,
        CANCELLATION_POLICY,
        COMPLAINT_HELP,
        PROVIDER_INFO,
        FAQ_PRICING,
        FAQ_BOOKING,
        GREETING,
        THANKS,
        UNKNOWN
    }

    public record IntentResult(
            IntentKind kind,
            List<String> keywords,
            List<String> synonymTokens,
            BigDecimal maxPrice,
            String city,
            boolean wantsPackages,
            String rawMessage
    ) {
    }

    /** Everyday wording -> catalogue wording. Verified against the DB afterwards. */
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("leak", "pipe"),
            Map.entry("leaking", "leakage"),
            Map.entry("tap", "tap"),
            Map.entry("faucet", "tap"),
            Map.entry("plumber", "plumbing"),
            Map.entry("electrician", "electric"),
            Map.entry("carpenter", "carpentry"),
            Map.entry("painter", "painting"),
            Map.entry("fridge", "refrigerator"),
            Map.entry("freezer", "refrigerator"),
            Map.entry("washing", "washing"),
            Map.entry("washer", "washing"),
            Map.entry("geyser", "geyser"),
            Map.entry("heater", "geyser"),
            Map.entry("ac", "ac"),
            Map.entry("a/c", "ac"),
            Map.entry("aircon", "ac"),
            Map.entry("airconditioner", "ac"),
            Map.entry("fridgecool", "cooling"),
            Map.entry("toilet", "toilet"),
            Map.entry("commode", "toilet"),
            Map.entry("washroom", "bathroom"),
            Map.entry("bath", "bathroom"),
            Map.entry("sofa", "sofa"),
            Map.entry("couch", "sofa"),
            Map.entry("pest", "pest"),
            Map.entry("cockroach", "cockroach"),
            Map.entry("mosquito", "mosquito"),
            Map.entry("termite", "termite"),
            Map.entry("haircut", "haircut"),
            Map.entry("salon", "hair"),
            Map.entry("beauty", "facial"),
            Map.entry("facial", "facial"),
            Map.entry("wifi", "wi-fi"),
            Map.entry("internet", "wi-fi"),
            Map.entry("laptop", "laptop"),
            Map.entry("computer", "computer"),
            Map.entry("printer", "printer"),
            Map.entry("mobile", "mobile"),
            Map.entry("phone", "mobile"),
            Map.entry("carwash", "car"),
            Map.entry("bike", "bike"),
            Map.entry("car", "car"),
            Map.entry("furniture", "furniture"),
            Map.entry("assembly", "assembly"),
            Map.entry("shifting", "shifting"),
            Map.entry("garden", "gardening"),
            Map.entry("deep", "deep"),
            Map.entry("clean", "clean"),
            Map.entry("cleaning", "cleaning")
    );

    private static final Set<String> STOPWORDS = Set.of(
            "i", "me", "my", "we", "our", "you", "your", "the", "a", "an", "is", "am", "are",
            "need", "want", "wanting", "please", "for", "to", "of", "in", "at", "on", "with",
            "and", "or", "can", "could", "would", "should", "do", "does", "have", "has", "get",
            "book", "booking", "looking", "look", "find", "show", "give", "help", "some", "any",
            "this", "that", "it", "there", "here", "how", "what", "when", "which", "who", "service",
            "services", "near", "around", "today", "tomorrow", "weekend"
    );

    private static final List<String> KNOWN_CITIES = List.of(
            "hyderabad", "secunderabad", "bengaluru", "bangalore", "mumbai", "delhi",
            "new delhi", "pune", "chennai", "kolkata", "ahmedabad", "jaipur", "kochi",
            "visakhapatnam", "vijayawada", "warangal", "noida", "gurugram", "indore", "lucknow"
    );

    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(?:under|below|less than|within|upto|up to|max|maximum|around)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d{2,6})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BARE_AMOUNT = Pattern.compile("(?:rs\\.?|inr|₹)\\s*(\\d{2,6})", Pattern.CASE_INSENSITIVE);

    public IntentResult parse(String message) {
        String original = message == null ? "" : message.trim();
        String lower = original.toLowerCase(Locale.ROOT);

        BigDecimal maxPrice = extractBudget(lower);
        String city = extractCity(lower);
        List<String> keywords = extractKeywords(lower);
        List<String> synonyms = new ArrayList<>();
        for (String token : keywords) {
            String mapped = SYNONYMS.get(token);
            if (mapped != null && !synonyms.contains(mapped) && !keywords.contains(mapped)) {
                synonyms.add(mapped);
            }
        }

        IntentKind kind = classify(lower, keywords);
        boolean wantsPackages = lower.contains("package") || lower.contains("packages")
                || lower.contains("price") || lower.contains("cost") || lower.contains("charges")
                || kind == IntentKind.PRICE_QUERY || kind == IntentKind.FAQ_PRICING;

        return new IntentResult(kind, keywords, synonyms, maxPrice, city, wantsPackages, original);
    }

    private BigDecimal extractBudget(String lower) {
        Matcher strong = BUDGET_PATTERN.matcher(lower);
        if (strong.find()) {
            return new BigDecimal(strong.group(1));
        }
        Matcher bare = BARE_AMOUNT.matcher(lower);
        if (bare.find()) {
            return new BigDecimal(bare.group(1));
        }
        return null;
    }

    private String extractCity(String lower) {
        for (String city : KNOWN_CITIES) {
            if (lower.contains(city)) {
                // Normalise the common alternate spellings.
                if (city.equals("bangalore")) {
                    return "Bengaluru";
                }
                if (city.equals("new delhi")) {
                    return "Delhi";
                }
                if (city.equals("gurugram")) {
                    return "Gurugram";
                }
                return capitalize(city);
            }
        }
        return null;
    }

    private List<String> extractKeywords(String lower) {
        String cleaned = lower.replaceAll("[^a-z0-9\\s/-]", " ");
        List<String> tokens = new ArrayList<>();
        for (String raw : cleaned.split("\\s+")) {
            if (raw.isBlank() || raw.length() < 2 || STOPWORDS.contains(raw)) {
                continue;
            }
            if (raw.matches("\\d+")) {
                continue;
            }
            if (!tokens.contains(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    private IntentKind classify(String lower, List<String> keywords) {
        if (lower.matches("^(hi|hello|hey|hola|namaste|good (morning|evening|afternoon))[!. ]*$")) {
            return IntentKind.GREETING;
        }
        if (lower.startsWith("thank") || lower.contains("thanks")) {
            return IntentKind.THANKS;
        }
        if (lower.contains("cancel") || lower.contains("refund")) {
            return IntentKind.CANCELLATION_POLICY;
        }
        if (lower.contains("complain") || lower.contains("complaint") || lower.contains("not happy")
                || lower.contains("bad service")) {
            return IntentKind.COMPLAINT_HELP;
        }
        if (lower.contains("become a provider") || lower.contains("register as provider")
                || lower.contains("work with fixora") || lower.contains("join as partner")) {
            return IntentKind.PROVIDER_INFO;
        }
        if (lower.contains("how much") || lower.contains("price") || lower.contains("cost")
                || lower.contains("charges") || lower.contains("rate")) {
            return IntentKind.PRICE_QUERY;
        }
        if (lower.contains("how do i book") || lower.contains("how to book") || lower.contains("booking process")
                || lower.contains("how does booking")) {
            return IntentKind.FAQ_BOOKING;
        }
        if (lower.contains("book") && keywords.isEmpty()) {
            return IntentKind.BOOKING_HELP;
        }
        if (!keywords.isEmpty()) {
            return IntentKind.FIND_SERVICE;
        }
        return IntentKind.UNKNOWN;
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
