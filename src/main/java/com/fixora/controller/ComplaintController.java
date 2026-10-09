package com.fixora.controller;

import com.fixora.dto.request.ComplaintRequestDTO;
import com.fixora.dto.response.ComplaintResponseDTO;
import com.fixora.service.ComplaintService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponseDTO create(@CurrentUserId Long customerId,
                                       @Valid @RequestBody ComplaintRequestDTO request) {
        return complaintService.create(customerId, request);
    }

    @GetMapping("/me")
    public List<ComplaintResponseDTO> myComplaints(@CurrentUserId Long customerId) {
        return complaintService.myComplaints(customerId);
    }
}
