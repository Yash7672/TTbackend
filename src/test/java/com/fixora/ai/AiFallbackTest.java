package com.fixora.ai;

import com.fixora.BaseDataTest;
import com.fixora.dto.request.*;
import com.fixora.dto.response.*;
import com.fixora.entity.Category;
import com.fixora.entity.Service;
import com.fixora.enums.ComplaintCategory;
import com.fixora.enums.PricingType;
import com.fixora.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests run with NO Anthropic key configured, which is exactly how the
 * application is expected to behave out of the box: the deterministic assistant
 * must still answer usefully from real catalogue data.
 */
class AiFallbackTest extends BaseDataTest {

    @Autowired private AiService aiService;

    private Service seedTapRepair() {
        Category plumbing = category("Plumbing");
        Service service = service(plumbing, "Tap Repair", "199", PricingType.STARTING_PRICE);
        servicePackage(service, "Standard Service", "199", 30);
        servicePackage(service, "Complete Service", "319", 60);
        return service;
    }

    @Test
    void statusReportsFallbackModeWhenNoKeyIsConfigured() {
        AiStatusDTO status = aiService.status();

        assertThat(status.activeMode()).isEqualTo("fallback");
        assertThat(status.apiKeyConfigured()).isFalse();
        assertThat(status.fallbackAvailable()).isTrue();
        assertThat(status.note()).contains("No ANTHROPIC_API_KEY");
    }

    @Test
    void chatMatchesARealServiceFromTheDatabase() {
        Service tapRepair = seedTapRepair();

        AiChatResponseDTO reply = aiService.chat(new AiChatRequestDTO("My bathroom tap is leaking", "Hyderabad"));

        assertThat(reply.mode()).isEqualTo("fallback");
        assertThat(reply.reply()).isNotBlank();
        assertThat(reply.reply()).contains("Tap Repair");
        assertThat(reply.suggestedServices()).anyMatch(s -> s.id().equals(tapRepair.getId()));
        assertThat(reply.suggestions()).isNotEmpty();
    }

    @Test
    void chatGivesDifferentAnswersForDifferentQuestions() {
        seedTapRepair();

        String serviceAnswer = aiService.chat(new AiChatRequestDTO("my tap is leaking", null)).reply();
        String greetingAnswer = aiService.chat(new AiChatRequestDTO("hello", null)).reply();
        String cancellationAnswer = aiService.chat(new AiChatRequestDTO("what is your cancellation policy?", null)).reply();

        assertThat(serviceAnswer).isNotEqualTo(greetingAnswer);
        assertThat(serviceAnswer).isNotEqualTo(cancellationAnswer);
        assertThat(cancellationAnswer.toLowerCase()).contains("cancel");
    }

    @Test
    void chatNeverInventsAServiceThatIsNotInTheCatalogue() {
        AiChatResponseDTO reply = aiService.chat(
                new AiChatRequestDTO("I need a moon-landing simulator installed", null));

        assertThat(reply.suggestedServices()).isEmpty();
        assertThat(reply.reply()).contains("could not match");
    }

    @Test
    void naturalLanguageSearchParsesABudgetAndReturnsDatabaseRows() {
        seedTapRepair();

        AiSearchResponseDTO result = aiService.search(
                new AiSearchRequestDTO("Find tap repair under 500 in Hyderabad", null, null));

        assertThat(result.mode()).isEqualTo("rules");
        assertThat(result.criteria().get("maxPrice")).isEqualTo(new BigDecimal("500"));
        assertThat(result.services()).isNotEmpty();
        assertThat(result.services()).allMatch(s -> s.basePrice().compareTo(new BigDecimal("500")) <= 0);
    }

    @Test
    void naturalLanguageSearchReportsAnEmptyResultHonestly() {
        AiSearchResponseDTO result = aiService.search(
                new AiSearchRequestDTO("submarine maintenance", null, null));

        assertThat(result.services()).isEmpty();
        assertThat(result.message()).contains("No Fixora service matches");
    }

    @Test
    void recommendationsAreScoreBasedAndExplainThemselves() {
        seedTapRepair();

        AiRecommendationResponseDTO result = aiService.recommendations(
                new AiRecommendationRequestDTO("I need tap repair", "Hyderabad", null, 5));

        assertThat(result.mode()).isEqualTo("rules");
        assertThat(result.recommendations()).isNotEmpty();
        assertThat(result.recommendations().get(0).reason()).contains("Recommended because");
        assertThat(result.explanation()).contains("rule-based");
    }

    @Test
    void bookingAssistanceExplainsRealPackages() {
        Service tapRepair = seedTapRepair();

        AiChatResponseDTO reply = aiService.bookingAssistance(
                new BookingAssistanceRequestDTO("I only need a basic check", tapRepair.getId(), null));

        assertThat(reply.reply()).contains("Standard Service");
        assertThat(reply.highlightedPackages()).isNotEmpty();
        assertThat(reply.reply()).contains("I will not create a booking");
    }

    @Test
    void complaintClassificationUsesRulesWhenNoModelIsAvailable() {
        ComplaintClassificationResponseDTO result = aiService.classifyComplaint(
                new ClassifyComplaintRequestDTO("The provider damaged my wall",
                        "While moving the fridge the technician chipped the wall and did not mention it.", null));

        assertThat(result.mode()).isEqualTo("fallback");
        assertThat(result.suggestedPriority()).isNotNull();
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void descriptionGeneratorReturnsAnUnpublishedDraft() {
        GeneratedDescriptionResponseDTO draft = aiService.generateServiceDescription(
                new GenerateDescriptionRequestDTO("AC General Service", "AC Services",
                        "Basic Inspection, Standard Service", "Filter cleaning", "Gas refill", PricingType.STARTING_PRICE));

        assertThat(draft.draft()).isTrue();
        assertThat(draft.shortDescription()).isNotBlank();
        assertThat(draft.fullDescription()).contains("AC General Service");
        assertThat(draft.faqSuggestions()).isNotEmpty();
    }

    @Test
    void bookingStatusIsNeverClaimedByTheAssistant() {
        seedTapRepair();

        AiChatResponseDTO reply = aiService.chat(new AiChatRequestDTO("did my booking get accepted?", null));

        // The assistant explains the flow instead of pretending to know a booking status.
        assertThat(reply.reply()).doesNotContain("your booking is accepted");
        assertThat(reply.suggestedServices()).isEmpty();
    }

    @Test
    void categoryEnumIsAlwaysAValidValueForComplaintTriage() {
        ComplaintClassificationResponseDTO result = aiService.classifyComplaint(
                new ClassifyComplaintRequestDTO("General question", "Something unclear happened.", null));

        assertThat(result.suggestedCategory()).isIn((Object[]) ComplaintCategory.values());
    }
}
