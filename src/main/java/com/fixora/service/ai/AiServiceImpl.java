package com.fixora.service.ai;

import com.fixora.config.AiProperties;
import com.fixora.dto.request.*;
import com.fixora.dto.response.*;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.mapper.ServiceMapper;
import com.fixora.repository.CategoryRepository;
import com.fixora.repository.ServicePackageRepository;
import com.fixora.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Orchestrates the AI features.
 *
 * Order of operations for every request:
 *   1. Interpret the request deterministically (IntentParser).
 *   2. Read real rows from MySQL (services, packages).
 *   3. Ask the configured provider to phrase an answer FROM those rows.
 *   4. On any provider error or missing key, answer deterministically instead.
 *
 * The application therefore never depends on an external API to start or to work.
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProperties aiProperties;
    private final IntentParser intentParser;
    private final ServiceMatcher serviceMatcher;
    private final RecommendationEngine recommendationEngine;
    private final DeterministicAssistant assistant;
    private final FallbackAiProvider fallbackProvider;
    private final AnthropicAiProvider anthropicProvider;
    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;
    private final CategoryRepository categoryRepository;
    private final ServiceMapper serviceMapper;

    private record Rendered(String text, String mode) {
    }

    // ---------------------------------------------------------------- 1. chat

    @Override
    @Transactional(readOnly = true)
    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        String message = request.message() == null ? "" : request.message().trim();
        String city = request.city();

        IntentParser.IntentResult intent = intentParser.parse(join(message, city));
        List<ServiceMatcher.Match> matches = serviceMatcher.match(intent, 4);
        List<Service> services = matches.stream().map(ServiceMatcher.Match::service).toList();
        Map<Long, List<ServicePackage>> packages = loadPackages(services, 3);

        String detectedCategory = services.isEmpty() || services.get(0).getCategory() == null
                ? null
                : services.get(0).getCategory().getName();

        Map<String, Object> facts = facts(intent, services, packages, city, detectedCategory);
        Rendered rendered = render("CHAT", message,
                "Answer the user's request using only the services and packages in the grounding data. "
                        + "Mention real prices with the Rs. prefix. Do not create a booking.",
                facts);

        return new AiChatResponseDTO(
                rendered.text(),
                rendered.mode(),
                activeProviderName(),
                detectedCategory,
                followUps(intent, services),
                serviceMapper.toDtoList(services),
                flatPackages(packages)
        );
    }

    // ------------------------------------------------------ 2. booking advice

    @Override
    @Transactional(readOnly = true)
    public AiChatResponseDTO bookingAssistance(BookingAssistanceRequestDTO request) {
        String message = request.message() == null ? "" : request.message().trim();

        Service service = null;
        if (request.serviceId() != null) {
            service = serviceRepository.findById(request.serviceId()).orElse(null);
        }
        if (service == null) {
            List<ServiceMatcher.Match> matches = serviceMatcher.match(intentParser.parse(message), 1);
            service = matches.isEmpty() ? null : matches.get(0).service();
        }

        List<ServicePackage> packages = service == null
                ? List.of()
                : packageRepository.findByServiceIdAndActiveTrueOrderByDisplayOrderAscPriceAsc(service.getId());

        Map<String, Object> facts = facts(intentParser.parse(message),
                service == null ? List.of() : List.of(service),
                service == null ? Map.of() : Map.of(service.getId(), packages),
                null,
                service == null || service.getCategory() == null ? null : service.getCategory().getName());
        facts.put("serviceId", service == null ? null : service.getId());
        facts.put("packageId", request.packageId());

        Rendered rendered = render("BOOKING_ASSISTANCE", message,
                "Explain the difference between the packages of this service using their real descriptions, "
                        + "prices and included/excluded work. Tell the user the booking must be submitted by them "
                        + "from the service page.",
                facts);

        return new AiChatResponseDTO(
                rendered.text(),
                rendered.mode(),
                activeProviderName(),
                service == null || service.getCategory() == null ? null : service.getCategory().getName(),
                List.of("Show me the cheapest package", "What is not included?", "How do I reschedule?"),
                service == null ? List.of() : serviceMapper.toDtoList(List.of(service)),
                serviceMapper.toPackageDtoList(packages)
        );
    }

    // ------------------------------------------------------------ 3. NL search

    @Override
    @Transactional(readOnly = true)
    public AiSearchResponseDTO search(AiSearchRequestDTO request) {
        String query = request.query() == null ? "" : request.query().trim();
        IntentParser.IntentResult intent = intentParser.parse(join(query, request.city()));

        BigDecimal maxPrice = request.maxPrice() != null ? request.maxPrice() : intent.maxPrice();

        List<ServiceMatcher.Match> matches = serviceMatcher.match(intent, 12);
        String matchedCategory = matches.isEmpty() || matches.get(0).service().getCategory() == null
                ? null
                : matches.get(0).service().getCategory().getName();
        Long categoryId = matches.isEmpty() || matches.get(0).service().getCategory() == null
                ? null
                : matches.get(0).service().getCategory().getId();

        // The database performs the filtering — never a hardcoded list.
        List<Service> results;
        if (categoryId != null && !matches.isEmpty() && matches.get(0).score() >= 3) {
            results = serviceRepository.search(categoryId, null, null, maxPrice,
                    PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "ratingAverage"))).getContent();
        } else {
            String keyword = longestKeyword(intent);
            results = serviceRepository.search(null, keyword, null, maxPrice,
                    PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "ratingAverage"))).getContent();
        }

        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("intent", intent.kind().name());
        criteria.put("keywords", intent.keywords());
        criteria.put("matchedCategory", matchedCategory);
        criteria.put("maxPrice", maxPrice);
        criteria.put("city", request.city() != null ? request.city() : intent.city());

        return new AiSearchResponseDTO(
                query,
                criteria,
                // No language model wrote this: the criteria and the results are deterministic.
                "rules",
                assistant.searchMessage(query, results.size(), matchedCategory, maxPrice),
                serviceMapper.toDtoList(results)
        );
    }

    // ------------------------------------------------------- 4. recommendations

    @Override
    @Transactional(readOnly = true)
    public AiRecommendationResponseDTO recommendations(AiRecommendationRequestDTO request) {
        String query = request.query() == null ? "" : request.query().trim();
        IntentParser.IntentResult intent = intentParser.parse(join(query, request.city()));
        int limit = request.limit() == null ? 6 : request.limit();

        List<RecommendationEngine.Scored> scored = recommendationEngine.recommend(intent, request.maxPrice(), limit);

        List<AiRecommendationResponseDTO.RecommendedService> recommendations = scored.stream()
                .map(s -> new AiRecommendationResponseDTO.RecommendedService(
                        serviceMapper.toDto(s.service()),
                        s.score(),
                        "Recommended because " + s.reason() + "."))
                .toList();

        String explanation = query.isBlank()
                ? "These are the most booked services in the Fixora catalogue right now."
                : "Selected by a transparent rule-based score over the live catalogue (keyword and category match, "
                  + "real review scores, popularity, featured flag and budget fit). No service is invented.";

        return new AiRecommendationResponseDTO("rules", explanation, recommendations);
    }

    // ---------------------------------------------- 5. description draft (admin)

    @Override
    @Transactional(readOnly = true)
    public GeneratedDescriptionResponseDTO generateServiceDescription(GenerateDescriptionRequestDTO request) {
        GeneratedDescriptionResponseDTO draft = assistant.draftDescription(request);

        if (!llmAvailable()) {
            return draft;
        }

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("serviceName", request.serviceName());
        facts.put("categoryName", request.categoryName());
        facts.put("packageNames", request.packageNames());
        facts.put("includedWork", request.includedWork());
        facts.put("excludedWork", request.excludedWork());
        facts.put("pricingType", request.pricingType() == null ? null : request.pricingType().name());
        facts.put("descriptionRequest", request);
        facts.put("deterministicDraft", draft.fullDescription());

        try {
            String polished = anthropicProvider.complete(new AiProvider.AiRequest(
                    "DESCRIPTION",
                    null,
                    "Rewrite the draft description as 3-5 clear sentences for customers. Keep every fact identical: "
                            + "same scope, same inclusions and exclusions, no invented prices.",
                    facts));
            return new GeneratedDescriptionResponseDTO(
                    draft.shortDescription(),
                    polished,
                    draft.faqSuggestions(),
                    draft.packageExplanation(),
                    true,               // still a draft: an admin must approve it
                    "anthropic");
        } catch (RuntimeException ex) {
            log.warn("Description generation fell back to deterministic draft: {}", ex.getMessage());
            return draft;
        }
    }

    // ------------------------------------------- 6. complaint classification

    @Override
    @Transactional(readOnly = true)
    public ComplaintClassificationResponseDTO classifyComplaint(ClassifyComplaintRequestDTO request) {
        ComplaintClassificationResponseDTO base = assistant.classify(request.subject(), request.description());

        if (!llmAvailable()) {
            return base;
        }

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("subject", request.subject());
        facts.put("description", request.description());
        facts.put("bookingRef", request.bookingRef());
        facts.put("suggestedCategory", base.suggestedCategory() == null ? null : base.suggestedCategory().name());
        facts.put("suggestedPriority", base.suggestedPriority() == null ? null : base.suggestedPriority().name());

        try {
            String summary = anthropicProvider.complete(new AiProvider.AiRequest(
                    "COMPLAINT_CLASSIFICATION",
                    null,
                    "Write two neutral sentences summarising this complaint for a support agent.",
                    facts));
            return new ComplaintClassificationResponseDTO(
                    base.suggestedCategory(),
                    base.suggestedPriority(),
                    summary,
                    "Suggested by the configured AI provider; an administrator confirms the final category and priority.",
                    "anthropic");
        } catch (RuntimeException ex) {
            log.warn("Complaint classification fell back to rules: {}", ex.getMessage());
            return base;
        }
    }

    // ------------------------------------------------------------------ status

    @Override
    @Transactional(readOnly = true)
    public AiStatusDTO status() {
        boolean keyConfigured = aiProperties.getAnthropic().hasKey();
        String mode = aiProperties.isAnthropicSelected() && keyConfigured ? "anthropic" : "fallback";

        return new AiStatusDTO(
                aiProperties.getProvider(),
                mode,
                aiProperties.resolvedModel(),
                keyConfigured,
                true,
                serviceRepository.count(),
                categoryRepository.count(),
                keyConfigured
                        ? "Anthropic is configured. If a call fails or times out, Fixora answers deterministically instead."
                        : "No ANTHROPIC_API_KEY is set, so the deterministic assistant is answering. "
                          + "Everything except the language-model phrasing works normally: search, recommendations, "
                          + "package explanations, FAQ answers and complaint triage all use real database data."
        );
    }

    // ----------------------------------------------------------------- helpers

    private Rendered render(String task, String userMessage, String instructions, Map<String, Object> facts) {
        AiProvider primary = aiProperties.isAnthropicSelected() ? anthropicProvider : fallbackProvider;

        if (primary.isLlm() && primary.isAvailable()) {
            try {
                String text = primary.complete(new AiProvider.AiRequest(task, userMessage, instructions, facts));
                if (text != null && !text.isBlank()) {
                    return new Rendered(text, "anthropic");
                }
                log.warn("Anthropic returned an empty reply; using the deterministic assistant.");
            } catch (RuntimeException ex) {
                log.warn("AI provider '{}' failed ({}). Falling back to deterministic answers.",
                        primary.name(), ex.getMessage());
            }
        }

        return new Rendered(
                fallbackProvider.complete(new AiProvider.AiRequest(task, userMessage, instructions, facts)),
                "fallback");
    }

    private boolean llmAvailable() {
        return aiProperties.isAnthropicSelected() && anthropicProvider.isAvailable();
    }

    private String activeProviderName() {
        return aiProperties.isAnthropicSelected() && anthropicProvider.isAvailable()
                ? AnthropicAiProvider.NAME
                : FallbackAiProvider.NAME;
    }

    private Map<String, Object> facts(IntentParser.IntentResult intent,
                                      List<Service> services,
                                      Map<Long, List<ServicePackage>> packages,
                                      String city,
                                      String detectedCategory) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("intent", intent.kind().name());
        facts.put("city", city == null ? "" : city);
        facts.put("budget", intent.maxPrice() == null ? "" : intent.maxPrice().toString());
        facts.put("category", detectedCategory == null ? "" : detectedCategory);
        facts.put("matchedServiceIds", services.stream().map(Service::getId).toList());
        facts.put("services", services.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("category", s.getCategory() == null ? "" : s.getCategory().getName());
            map.put("startingPrice", s.getBasePrice());
            map.put("pricingType", s.getPricingType() == null ? "" : s.getPricingType().name());
            map.put("durationMinutes", s.getDurationMinutes());
            map.put("rating", s.getRatingCount() != null && s.getRatingCount() > 0 ? s.getRatingAverage() : "no reviews yet");
            map.put("ratingCount", s.getRatingCount());
            return map;
        }).toList());

        List<Map<String, Object>> packageFacts = new ArrayList<>();
        packages.forEach((serviceId, list) -> {
            for (ServicePackage pkg : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", pkg.getId());
                map.put("serviceId", serviceId);
                map.put("name", pkg.getName());
                map.put("price", pkg.getPrice());
                map.put("durationMinutes", pkg.getDurationMinutes());
                map.put("pricingType", pkg.getPricingType() == null ? "" : pkg.getPricingType().name());
                map.put("includedWork", pkg.getIncludedWork());
                map.put("excludedWork", pkg.getExcludedWork());
                packageFacts.add(map);
            }
        });
        facts.put("packages", packageFacts);
        return facts;
    }

    private Map<Long, List<ServicePackage>> loadPackages(List<Service> services, int perService) {
        Map<Long, List<ServicePackage>> packages = new LinkedHashMap<>();
        for (Service service : services) {
            packages.put(service.getId(), packageRepository
                    .findByServiceIdAndActiveTrueOrderByDisplayOrderAscPriceAsc(service.getId())
                    .stream().limit(perService).toList());
        }
        return packages;
    }

    private List<ServicePackageResponseDTO> flatPackages(Map<Long, List<ServicePackage>> packages) {
        List<ServicePackage> flat = new ArrayList<>();
        packages.values().forEach(flat::addAll);
        return serviceMapper.toPackageDtoList(flat);
    }

    private List<String> followUps(IntentParser.IntentResult intent, List<Service> services) {
        List<String> suggestions = new ArrayList<>();
        if (!services.isEmpty()) {
            suggestions.add("What packages are available for " + services.get(0).getName() + "?");
            suggestions.add("How much does " + services.get(0).getName() + " cost?");
        } else {
            suggestions.add("I need a plumber for a leaking tap");
            suggestions.add("Find AC service for this weekend");
        }
        suggestions.add("How does cancellation work?");
        return suggestions;
    }

    private String longestKeyword(IntentParser.IntentResult intent) {
        return intent.keywords().stream()
                .filter(k -> k.length() >= 3)
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
    }

    private String join(String message, String city) {
        if (city == null || city.isBlank()) {
            return message;
        }
        return (message == null ? "" : message) + " in " + city;
    }
}
