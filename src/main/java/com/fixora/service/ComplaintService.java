package com.fixora.service;

import com.fixora.dto.request.ComplaintRequestDTO;
import com.fixora.dto.request.ComplaintStatusRequestDTO;
import com.fixora.dto.response.ComplaintResponseDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.enums.ComplaintStatus;

import java.util.List;

public interface ComplaintService {

    ComplaintResponseDTO create(Long customerId, ComplaintRequestDTO request);

    List<ComplaintResponseDTO> myComplaints(Long customerId);

    PageResponseDTO<ComplaintResponseDTO> allComplaints(ComplaintStatus status, int page, int size);

    ComplaintResponseDTO updateStatus(Long complaintId, ComplaintStatusRequestDTO request);
}
