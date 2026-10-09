package com.fixora.service.ai;

import com.fixora.dto.request.GenerateDescriptionRequestDTO;
import com.fixora.entity.Service;
import com.fixora.entity.ServicePackage;
import com.fixora.repository.ServicePackageRepository;
import com.fixora.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Always-available provider. Selected with AI_PROVIDER=fallback (the default) and
 * ALSO used as the safety net whenever the language-model provider fails.
 *
 * It is not an LLM and every response says so through the `mode` field the caller
 * reports. It answers from real catalogue rows only.
 */
@Component
@RequiredArgsConstructor
public class FallbackAiProvider implements AiProvider {

    public static final String NAME = "fallback";

    private final IntentParser intentParser;
    private final ServiceMatcher serviceMatcher;
    private final DeterministicAssistant assistant;
    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isLlm() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String complete(AiRequest request) {
        Map<String, Object> facts = request.facts() == null ? Map.of() : request.facts();

        return switch (request.task() == null ? "CHAT" : request.task()) {
            case "CHAT" -> chat(request, facts);
            case "BOOKING_ASSISTANCE" -> bookingAssistance(request, facts);
            case "DESCRIPTION" -> description(facts);
            case "COMPLAINT_CLASSIFICATION" -> classification(facts);
            default -> "The Fixora assistant can match services, explain packages and describe how booking works. "
                    + "Ask about a service such as cleaning, AC service or laptop repair.";
        };
    }

    // ------------------------------------------------------------------ tasks

    private String chat(AiRequest request, Map<String, Object> facts) {
        String city = asString(facts.get("city"));
        IntentParser.IntentResult intent = intentParser.parse(join(request.userMessage(), city));

        List<Service> services = resolveServices(facts, intent, 4);
        Map<Long, List<ServicePackage>> packages = loadPackages(services, 3);
        String detectedCategory = services.isEmpty() || services.get(0).getCategory() == null
                ? asString(facts.get("category"))
                : services.get(0).getCategory().getName();

        return assistant.chatReply(intent, services, packages, city, detectedCategory);
    }

    private String bookingAssistance(AiRequest request, Map<String, Object> facts) {
        Service service = null;
        Object serviceId = facts.get("serviceId");
        if (serviceId instanceof Long id) {
            service = serviceRepository.findById(id).orElse(null);
        }
        if (service == null) {
            IntentParser.IntentResult intent = intentParser.parse(request.userMessage());
            List<ServiceMatcher.Match> matches = serviceMatcher.match(intent, 1);
            service = matches.isEmpty() ? null : matches.get(0).service();
        }

        List<ServicePackage> packages = service == null
                ? List.of()
                : packageRepository.findByServiceIdAndActiveTrueOrderByDisplayOrderAscPriceAsc(service.getId());

        return assistant.bookingAssistance(service, packages, request.userMessage());
    }

    private String description(Map<String, Object> facts) {
        Object draft = facts.get("descriptionRequest");
        if (draft instanceof GenerateDescriptionRequestDTO dto) {
            return assistant.draftDescription(dto).fullDescription();
        }
        return "Provide the service name, category and package details so a draft description can be generated.";
    }

    private String classification(Map<String, Object> facts) {
        return assistant.classify(asString(facts.get("subject")), asString(facts.get("description"))).summary();
    }

    // ---------------------------------------------------------------- helpers

    private List<Service> resolveServices(Map<String, Object> facts, IntentParser.IntentResult intent, int limit) {
        Object ids = facts.get("matchedServiceIds");
        List<Service> services = new ArrayList<>();
        if (ids instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof Long id) {
                    serviceRepository.findById(id).ifPresent(services::add);
                }
            }
        }
        if (services.isEmpty()) {
            services.addAll(serviceMatcher.match(intent, limit).stream()
                    .map(ServiceMatcher.Match::service).toList());
        }
        return services.stream().limit(limit).toList();
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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String join(String message, String city) {
        if (city == null || city.isBlank()) {
            return message;
        }
        return (message == null ? "" : message) + " in " + city;
    }
}
