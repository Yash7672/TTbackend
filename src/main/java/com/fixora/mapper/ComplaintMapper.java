package com.fixora.mapper;

import com.fixora.dto.response.ComplaintResponseDTO;
import com.fixora.entity.Complaint;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public ComplaintResponseDTO toDto(Complaint complaint) {
        return new ComplaintResponseDTO(
                complaint.getId(),
                complaint.getSubject(),
                complaint.getDescription(),
                complaint.getCategory(),
                complaint.getStatus(),
                complaint.getPriority(),
                complaint.getBooking() != null ? complaint.getBooking().getId() : null,
                complaint.getBooking() != null ? complaint.getBooking().getBookingRef() : null,
                complaint.getCustomer() != null ? complaint.getCustomer().getId() : null,
                complaint.getCustomer() != null ? complaint.getCustomer().getFullName() : null,
                complaint.getAiSuggestedCategory(),
                complaint.getAiSuggestedSummary(),
                complaint.getResolutionNote(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt()
        );
    }
}
