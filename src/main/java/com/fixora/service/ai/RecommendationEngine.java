package com.fixora.service.ai;

import com.fixora.entity.Service;
import com.fixora.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Smart recommendations with a transparent, rule-based score.
 *
 * The mode is reported as "rules" because that is what it is: a deterministic
 * blend of keyword match, category match, real review scores, popularity,
 * featured flag and budget fit. A language model may later rephrase the reasons,
 * but the selection itself stays explainable and never invents a service.
 */
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final ServiceRepository serviceRepository;
    private final ServiceMatcher serviceMatcher;

    public record Scored(Service service, int score, String reason) {
    }

    @Transactional(readOnly = true)
    public List<Scored> recommend(IntentParser.IntentResult intent, BigDecimal maxPrice, int limit) {
        Map<Long, Scored> candidates = new LinkedHashMap<>();

        // 1. Direct keyword/category matches carry the strongest signal.
        for (ServiceMatcher.Match match : serviceMatcher.match(intent, 20)) {
            Service service = match.service();
            int score = 25 + match.score() * 6;
            List<String> reasons = new ArrayList<>();
            reasons.add("it matches your request (matched on '" + String.join("', '", match.matchedOn()) + "')");
            candidates.put(service.getId(), new Scored(service, score, String.join("; ", reasons)));
        }

        // 2. Popular + featured services broaden the pool for vague requests.
        for (Service service : serviceRepository.findTop8ByActiveTrueOrderByRatingCountDescRatingAverageDesc()) {
            candidates.computeIfAbsent(service.getId(),
                    s -> new Scored(service, 3, "it is one of the most booked services on Fixora"));
        }
        for (Service service : serviceRepository.findByActiveTrueAndFeaturedTrueOrderByRatingAverageDescIdAsc()) {
            candidates.computeIfAbsent(service.getId(),
                    s -> new Scored(service, 2, "it is a featured Fixora service"));
        }

        List<Scored> scored = new ArrayList<>();
        for (Scored candidate : candidates.values()) {
            Service service = candidate.service();
            int score = candidate.score();
            List<String> reasons = new ArrayList<>();
            reasons.add(candidate.reason());

            if (service.getRatingAverage() != null && service.getRatingCount() != null && service.getRatingCount() > 0) {
                score += Math.min(25, service.getRatingAverage().intValue() * 5);
                reasons.add("customers rate it " + service.getRatingAverage() + "/5 across "
                        + service.getRatingCount() + " reviews");
            }
            if (Boolean.TRUE.equals(service.getFeatured())) {
                score += 5;
            }
            if (maxPrice != null && service.getBasePrice() != null) {
                if (service.getBasePrice().compareTo(maxPrice) <= 0) {
                    score += 15;
                    reasons.add("it starts at Rs." + service.getBasePrice() + ", inside your budget of Rs." + maxPrice);
                } else {
                    score -= 30;
                    reasons.add("note: it starts at Rs." + service.getBasePrice() + ", above your stated budget");
                }
            }
            if (intent.city() != null && service.getCategory() != null) {
                score += 2; // city is applied to providers, keep the service relevant
            }

            scored.add(new Scored(service, score, reasons.get(0)
                    + (reasons.size() > 1 ? "; " + String.join("; ", reasons.subList(1, reasons.size())) : "")));
        }

        scored.sort(Comparator.comparingInt(Scored::score).reversed());

        List<Scored> bounded = scored.stream().limit(Math.max(limit, 1)).toList();
        if (bounded.isEmpty()) {
            // Never return nothing: fall back to the live catalogue.
            return serviceRepository.findAll(PageRequest.of(0, Math.max(limit, 1))).getContent().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActive()))
                    .map(s -> new Scored(s, 1, "it is available in the Fixora catalogue right now"))
                    .toList();
        }
        return bounded;
    }
}
