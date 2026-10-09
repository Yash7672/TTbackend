package com.fixora.service.impl;

import com.fixora.dto.request.ClassifyComplaintRequestDTO;
import com.fixora.dto.request.ComplaintRequestDTO;
import com.fixora.dto.request.ComplaintStatusRequestDTO;
import com.fixora.dto.response.ComplaintClassificationResponseDTO;
import com.fixora.dto.response.ComplaintResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.entity.Booking;
import com.fixora.entity.Complaint;
import com.fixora.entity.User;
import com.fixora.enums.ComplaintStatus;
import com.fixora.enums.NotificationType;
import com.fixora.exception.BadRequestException;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.exception.UnauthorizedActionException;
import com.fixora.mapper.ComplaintMapper;
import com.fixora.repository.BookingRepository;
import com.fixora.repository.ComplaintRepository;
import com.fixora.repository.UserRepository;
import com.fixora.service.ComplaintService;
import com.fixora.service.NotificationService;
import com.fixora.service.ai.AiService;
import com.fixora.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ComplaintMapper complaintMapper;
    private final NotificationService notificationService;
    private final AiService aiService;

    @Override
    @Transactional
    public ComplaintResponseDTO create(Long customerId, ComplaintRequestDTO request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", customerId));

        Booking booking = null;
        if (request.bookingId() != null) {
            booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Booking", request.bookingId()));
            if (!booking.getCustomer().getId().equals(customerId)) {
                throw new UnauthorizedActionException("You can only complain about your own bookings.");
            }
        }

        Complaint complaint = Complaint.builder()
                .customer(customer)
                .booking(booking)
                .subject(request.subject().trim())
                .description(request.description().trim())
                .category(request.category())
                .status(ComplaintStatus.OPEN)
                .build();

        // AI suggestion is advisory. It never sets the final category or status.
        try {
            ComplaintClassificationResponseDTO suggestion = aiService.classifyComplaint(
                    new ClassifyComplaintRequestDTO(request.subject(), request.description(),
                            booking == null ? null : booking.getBookingRef()));
            if (suggestion != null) {
                complaint.setAiSuggestedCategory(suggestion.suggestedCategory() == null
                        ? null : suggestion.suggestedCategory().name());
                complaint.setAiSuggestedSummary(suggestion.summary());
                if (complaint.getCategory() == null && suggestion.suggestedCategory() != null) {
                    complaint.setCategory(suggestion.suggestedCategory());
                }
            }
        } catch (RuntimeException ex) {
            // A complaint must always be recorded, even if the AI layer misbehaves.
            log.warn("Complaint classification unavailable: {}", ex.getMessage());
        }

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint {} created by customer {}", saved.getId(), customerId);
        return complaintMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> myComplaints(Long customerId) {
        return complaintRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(complaintMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ComplaintResponseDTO> allComplaints(ComplaintStatus status, int page, int size) {
        var result = status == null
                ? complaintRepository.findAllByOrderByCreatedAtDesc(PageUtils.of(page, size))
                : complaintRepository.findByStatusOrderByCreatedAtDesc(status, PageUtils.of(page, size));
        return PageResponseDTO.of(result, complaintMapper::toDto);
    }

    @Override
    @Transactional
    public ComplaintResponseDTO updateStatus(Long complaintId, ComplaintStatusRequestDTO request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> ResourceNotFoundException.of("Complaint", complaintId));

        if (request.status() == null) {
            throw new BadRequestException("A target status is required.");
        }

        complaint.setStatus(request.status());
        if (request.priority() != null) {
            complaint.setPriority(request.priority());
        }
        if (request.category() != null) {
            complaint.setCategory(request.category());
        }
        if (request.resolutionNote() != null) {
            complaint.setResolutionNote(request.resolutionNote());
        }

        Complaint saved = complaintRepository.save(complaint);

        notificationService.notifyUser(complaint.getCustomer(), NotificationType.COMPLAINT_STATUS_CHANGED,
                "Complaint " + saved.getId() + " is now " + saved.getStatus(),
                request.resolutionNote() == null
                        ? "Your complaint status was updated."
                        : "Your complaint status was updated: " + request.resolutionNote(),
                complaint.getBooking() == null ? null : complaint.getBooking().getId());

        return complaintMapper.toDto(saved);
    }
}
