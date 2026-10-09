package com.fixora.service.ai;

import com.fixora.dto.request.*;
import com.fixora.dto.response.*;

/** Public AI surface. Every method is safe to call with no external key configured. */
public interface AiService {

    AiChatResponseDTO chat(AiChatRequestDTO request);

    AiChatResponseDTO bookingAssistance(BookingAssistanceRequestDTO request);

    AiSearchResponseDTO search(AiSearchRequestDTO request);

    AiRecommendationResponseDTO recommendations(AiRecommendationRequestDTO request);

    GeneratedDescriptionResponseDTO generateServiceDescription(GenerateDescriptionRequestDTO request);

    ComplaintClassificationResponseDTO classifyComplaint(ClassifyComplaintRequestDTO request);

    AiStatusDTO status();
}
