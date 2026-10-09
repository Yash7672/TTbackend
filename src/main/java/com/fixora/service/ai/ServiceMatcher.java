package com.fixora.service.ai;

import com.fixora.entity.Category;
import com.fixora.entity.Service;
import com.fixora.repository.CategoryRepository;
import com.fixora.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Finds real services for a parsed request.
 *
 * Everything here is a database lookup — the matcher never returns a service that
 * is not in MySQL, and never guesses a price.
 */
@Component
@RequiredArgsConstructor
public class ServiceMatcher {

    private final ServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;

    public record Match(Service service, int score, List<String> matchedOn) {
    }

    public List<Match> match(IntentParser.IntentResult intent, int limit) {
        Map<Long, Integer> scores = new LinkedHashMap<>();
        Map<Long, Service> services = new LinkedHashMap<>();
        Map<Long, LinkedHashSet<String>> matchedOn = new LinkedHashMap<>();

        for (String keyword : intent.keywords()) {
            for (Service service : safeSearch(keyword)) {
                register(services, scores, matchedOn, service, 3, keyword);
            }
        }
        for (String synonym : intent.synonymTokens()) {
            for (Service service : safeSearch(synonym)) {
                register(services, scores, matchedOn, service, 2, synonym);
            }
        }

        // Category-name hits boost every service inside that category.
        for (String keyword : allTokens(intent)) {
            for (Category category : categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()) {
                if (category.getName() != null && category.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
                    for (Service service : serviceRepository.findByCategoryIdAndActiveTrue(
                            category.getId(), org.springframework.data.domain.PageRequest.of(0, 6)).getContent()) {
                        register(services, scores, matchedOn, service, 2, category.getName());
                    }
                }
            }
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(Math.max(limit, 1))
                .map(entry -> new Match(
                        services.get(entry.getKey()),
                        entry.getValue(),
                        new ArrayList<>(matchedOn.get(entry.getKey()))))
                .toList();
    }

    /** Cheapest-to-priciest "starting from" number across a set of matches. */
    public java.math.BigDecimal lowestPrice(List<Match> matches) {
        return matches.stream()
                .map(m -> m.service().getBasePrice())
                .filter(Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
    }

    private List<String> allTokens(IntentParser.IntentResult intent) {
        List<String> tokens = new ArrayList<>(intent.keywords());
        intent.synonymTokens().stream().filter(t -> !tokens.contains(t)).forEach(tokens::add);
        return tokens;
    }

    private List<Service> safeSearch(String token) {
        if (token == null || token.length() < 3) {
            // Short tokens such as "ac" are handled by the category pass.
            if (token != null && token.equals("ac")) {
                return serviceRepository.findByActiveTrueAndNameContainingIgnoreCase("AC");
            }
            return List.of();
        }
        return serviceRepository.findByActiveTrueAndNameContainingIgnoreCase(token);
    }

    private void register(Map<Long, Service> services,
                          Map<Long, Integer> scores,
                          Map<Long, LinkedHashSet<String>> matchedOn,
                          Service service,
                          int weight,
                          String token) {
        services.put(service.getId(), service);
        scores.merge(service.getId(), weight, Integer::sum);
        matchedOn.computeIfAbsent(service.getId(), k -> new LinkedHashSet<>()).add(token);
    }
}
