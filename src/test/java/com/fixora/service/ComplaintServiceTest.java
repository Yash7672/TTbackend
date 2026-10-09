package com.fixora.service;

import com.fixora.BaseDataTest;
import com.fixora.dto.request.ComplaintRequestDTO;
import com.fixora.dto.request.ComplaintStatusRequestDTO;
import com.fixora.dto.response.ComplaintResponseDTO;
import com.fixora.entity.*;
import com.fixora.enums.*;
import com.fixora.exception.UnauthorizedActionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplaintServiceTest extends BaseDataTest {

    @Autowired private ComplaintService complaintService;
    @Autowired private NotificationService notificationService;

    @Test
    void createsAComplaintAndStoresAnAiSuggestion() {
        User customer = customer();

        ComplaintResponseDTO complaint = complaintService.create(customer.getId(),
                new ComplaintRequestDTO("Provider arrived very late",
                        "The technician arrived two hours after the slot and did not inform me.", null, null));

        assertThat(complaint.id()).isNotNull();
        assertThat(complaint.status()).isEqualTo(ComplaintStatus.OPEN);
        // Deterministic classifier recognised a late arrival.
        assertThat(complaint.category()).isEqualTo(ComplaintCategory.LATE_ARRIVAL);
        assertThat(complaint.aiSuggestedSummary()).isNotBlank();
    }

    @Test
    void aCustomerCannotAttachSomeoneElsesBooking() {
        User customer = customer();
        User other = customer();
        Provider provider = provider(ProviderStatus.APPROVED);
        Service service = service(category("Home Cleaning"), "Balcony Cleaning", "399", PricingType.FIXED_PRICE);
        ServicePackage pkg = servicePackage(service, "Basic", "399", 45);
        Address address = address(other, true);
        Booking booking = completedBooking(other, provider, service, pkg, address);

        Long bookingId = booking.getId();
        assertThatThrownBy(() -> complaintService.create(customer.getId(),
                new ComplaintRequestDTO("Not mine", "I should not be able to attach this booking.", bookingId, null)))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void adminCanResolveAComplaintAndTheCustomerIsNotified() {
        User customer = customer();
        ComplaintResponseDTO complaint = complaintService.create(customer.getId(),
                new ComplaintRequestDTO("Overcharged", "I was charged more than the package price.", null, null));

        assertThat(complaint.category()).isEqualTo(ComplaintCategory.PRICING);

        ComplaintResponseDTO updated = complaintService.updateStatus(complaint.id(),
                new ComplaintStatusRequestDTO(ComplaintStatus.RESOLVED, ComplaintPriority.MEDIUM,
                        ComplaintCategory.PRICING, "Partial credit issued by support."));

        assertThat(updated.status()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(updated.resolutionNote()).isEqualTo("Partial credit issued by support.");
        assertThat(updated.priority()).isEqualTo(ComplaintPriority.MEDIUM);

        assertThat(notificationService.recent(customer.getId()))
                .anyMatch(n -> n.type() == NotificationType.COMPLAINT_STATUS_CHANGED);
    }

    @Test
    void customersOnlySeeTheirOwnComplaints() {
        User customer = customer();
        User other = customer();
        complaintService.create(customer.getId(),
                new ComplaintRequestDTO("Mine only", "This complaint belongs to the first customer.", null, null));

        assertThat(complaintService.myComplaints(other.getId())).isEmpty();
        assertThat(complaintService.myComplaints(customer.getId())).hasSize(1);
    }
}
