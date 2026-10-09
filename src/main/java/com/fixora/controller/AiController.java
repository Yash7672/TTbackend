package com.fixora.controller;

import com.fixora.dto.request.*;
import com.fixora.dto.response.*;
import com.fixora.service.ai.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI endpoints. All of them work with no external API key configured — the
 * deterministic engine answers instead, and the `mode` field says which one ran.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public AiChatResponseDTO chat(@Valid @RequestBody AiChatRequestDTO request) {
        return aiService.chat(request);
    }

    @PostMapping("/search")
    public AiSearchResponseDTO search(@Valid @RequestBody AiSearchRequestDTO request) {
        return aiService.search(request);
    }

    @PostMapping("/recommendations")
    public AiRecommendationResponseDTO recommendations(@Valid @RequestBody AiRecommendationRequestDTO request) {
        return aiService.recommendations(request);
    }

    @PostMapping("/booking-assistance")
    public AiChatResponseDTO bookingAssistance(@Valid @RequestBody BookingAssistanceRequestDTO request) {
        return aiService.bookingAssistance(request);
    }

    @PostMapping("/generate-service-description")
    public GeneratedDescriptionResponseDTO generateServiceDescription(
            @Valid @RequestBody GenerateDescriptionRequestDTO request) {
        return aiService.generateServiceDescription(request);
    }

    @PostMapping("/classify-complaint")
    public ComplaintClassificationResponseDTO classifyComplaint(
            @Valid @RequestBody ClassifyComplaintRequestDTO request) {
        return aiService.classifyComplaint(request);
    }

    @GetMapping("/status")
    public AiStatusDTO status() {
        return aiService.status();
    }
}
