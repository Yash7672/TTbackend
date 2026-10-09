package com.fixora.controller;

import com.fixora.dto.request.*;
import com.fixora.dto.response.*;
import com.fixora.enums.BookingStatus;
import com.fixora.enums.ComplaintStatus;
import com.fixora.enums.ProviderStatus;
import com.fixora.enums.UserRole;
import com.fixora.service.AdminService;
import com.fixora.service.BookingService;
import com.fixora.service.ComplaintService;
import com.fixora.service.ProviderManager;
import com.fixora.service.ReviewService;
import com.fixora.service.UserService;
import com.fixora.service.ai.AiService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin surface.
 *
 * Every method resolves @CurrentUserId(adminOnly = true), which rejects a
 * non-admin caller. Frontend role checks are a usability feature only — this is
 * the server-side check that actually matters.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final ProviderManager providerManager;
    private final BookingService bookingService;
    private final ComplaintService complaintService;
    private final ReviewService reviewService;
    private final AiService aiService;

    // --------------------------------------------------------------- dashboard

    @GetMapping("/dashboard")
    public AdminDashboardResponseDTO dashboard(@CurrentUserId(adminOnly = true) Long adminId) {
        return adminService.dashboard();
    }

    @GetMapping("/ai/status")
    public AiStatusDTO aiStatus(@CurrentUserId(adminOnly = true) Long adminId) {
        return aiService.status();
    }

    // ------------------------------------------------------------------ users

    @GetMapping("/users")
    public PageResponseDTO<UserResponseDTO> users(@CurrentUserId(adminOnly = true) Long adminId,
                                                  @RequestParam(required = false) UserRole role,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(role, page, size);
    }

    // -------------------------------------------------------------- providers

    @GetMapping("/providers")
    public PageResponseDTO<ProviderResponseDTO> providers(@CurrentUserId(adminOnly = true) Long adminId,
                                                          @RequestParam(required = false) ProviderStatus status,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return providerManager.listProviders(status, page, size);
    }

    @PutMapping("/providers/{id}/status")
    public ProviderResponseDTO verifyProvider(@CurrentUserId(adminOnly = true) Long adminId,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ProviderStatusRequestDTO request) {
        return providerManager.updateVerification(id, request.status(), request.note());
    }

    // --------------------------------------------------------------- bookings

    @GetMapping("/bookings")
    public PageResponseDTO<BookingResponseDTO> bookings(@CurrentUserId(adminOnly = true) Long adminId,
                                                        @RequestParam(required = false) BookingStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return bookingService.allBookings(status, page, size);
    }

    @PutMapping("/bookings/{id}/assign")
    public BookingResponseDTO assign(@CurrentUserId(adminOnly = true) Long adminId,
                                     @PathVariable Long id,
                                     @RequestParam Long providerId) {
        return bookingService.assignProvider(id, providerId);
    }

    // -------------------------------------------------------------- catalogue

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDTO createCategory(@CurrentUserId(adminOnly = true) Long adminId,
                                              @Valid @RequestBody CategoryRequestDTO request) {
        return adminService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponseDTO updateCategory(@CurrentUserId(adminOnly = true) Long adminId,
                                              @PathVariable Long id,
                                              @Valid @RequestBody CategoryRequestDTO request) {
        return adminService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@CurrentUserId(adminOnly = true) Long adminId, @PathVariable Long id) {
        adminService.deleteCategory(id);
    }

    @GetMapping("/services")
    public PageResponseDTO<ServiceResponseDTO> services(@CurrentUserId(adminOnly = true) Long adminId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return adminService.listAllServices(page, size);
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponseDTO createService(@CurrentUserId(adminOnly = true) Long adminId,
                                            @Valid @RequestBody ServiceRequestDTO request) {
        return adminService.createService(request);
    }

    @PutMapping("/services/{id}")
    public ServiceResponseDTO updateService(@CurrentUserId(adminOnly = true) Long adminId,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ServiceRequestDTO request) {
        return adminService.updateService(id, request);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@CurrentUserId(adminOnly = true) Long adminId, @PathVariable Long id) {
        adminService.deleteService(id);
    }

    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public ServicePackageResponseDTO createPackage(@CurrentUserId(adminOnly = true) Long adminId,
                                                   @Valid @RequestBody PackageRequestDTO request) {
        return adminService.createPackage(request);
    }

    @PutMapping("/packages/{id}")
    public ServicePackageResponseDTO updatePackage(@CurrentUserId(adminOnly = true) Long adminId,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody PackageRequestDTO request) {
        return adminService.updatePackage(id, request);
    }

    @DeleteMapping("/packages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePackage(@CurrentUserId(adminOnly = true) Long adminId, @PathVariable Long id) {
        adminService.deletePackage(id);
    }

    // -------------------------------------------------------------- complaints

    @GetMapping("/complaints")
    public PageResponseDTO<ComplaintResponseDTO> complaints(@CurrentUserId(adminOnly = true) Long adminId,
                                                            @RequestParam(required = false) ComplaintStatus status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return complaintService.allComplaints(status, page, size);
    }

    @PutMapping("/complaints/{id}/status")
    public ComplaintResponseDTO updateComplaint(@CurrentUserId(adminOnly = true) Long adminId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody ComplaintStatusRequestDTO request) {
        return complaintService.updateStatus(id, request);
    }

    // ----------------------------------------------------------- review moderation

    @GetMapping("/reviews")
    public PageResponseDTO<ReviewResponseDTO> reviews(@CurrentUserId(adminOnly = true) Long adminId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return reviewService.allReviews(page, size);
    }

    @PutMapping("/reviews/{id}/visibility")
    public ReviewResponseDTO setReviewVisibility(@CurrentUserId(adminOnly = true) Long adminId,
                                                 @PathVariable Long id,
                                                 @RequestParam boolean visible) {
        return reviewService.setVisibility(id, visible);
    }

    /** Simple echo of the admin's own identity — handy when debugging the header flow. */
    @GetMapping("/whoami")
    public Map<String, Object> whoami(@CurrentUserId(adminOnly = true) Long adminId) {
        return Map.of("adminId", adminId, "role", "ADMIN");
    }
}
