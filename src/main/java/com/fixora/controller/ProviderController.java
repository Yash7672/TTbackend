package com.fixora.controller;

import com.fixora.dto.request.AvailabilityRequestDTO;
import com.fixora.dto.request.ProviderRequestDTO;
import com.fixora.dto.request.ProviderServiceRequestDTO;
import com.fixora.dto.response.*;
import com.fixora.enums.ProviderStatus;
import com.fixora.service.ProviderManager;
import com.fixora.service.ReviewService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public provider discovery plus the provider's own dashboard endpoints.
 * Only ADMIN-approved providers are ever returned by the public endpoints.
 */
@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderManager providerManager;
    private final ReviewService reviewService;

    // ------------------------------------------------------------- discovery

    @GetMapping
    public PageResponseDTO<ProviderResponseDTO> list(@RequestParam(required = false) ProviderStatus status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "12") int size) {
        // Public listing is always restricted to approved providers.
        return providerManager.listProviders(ProviderStatus.APPROVED, page, size);
    }

    @GetMapping("/search")
    public PageResponseDTO<ProviderResponseDTO> search(@RequestParam(required = false) String city,
                                                      @RequestParam(required = false) String area,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long serviceId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "12") int size) {
        return providerManager.searchProviders(city, area, keyword, serviceId, page, size);
    }

    @GetMapping("/{id}")
    public ProviderResponseDTO get(@PathVariable Long id) {
        return providerManager.getProvider(id);
    }

    @GetMapping("/{id}/services")
    public List<ProviderServiceResponseDTO> services(@PathVariable Long id) {
        return providerManager.listProviderServices(id);
    }

    @GetMapping("/{id}/availability")
    public List<AvailabilityResponseDTO> availability(@PathVariable Long id) {
        return providerManager.listAvailability(id);
    }

    @GetMapping("/{id}/reviews")
    public PageResponseDTO<ReviewResponseDTO> reviews(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return reviewService.reviewsForProvider(id, page, size);
    }

    // ------------------------------------------------------- my provider area

    @GetMapping("/me")
    public ProviderResponseDTO myProfile(@CurrentUserId Long userId) {
        return providerManager.getMyProfile(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderResponseDTO createMyProfile(@CurrentUserId Long userId,
                                               @Valid @RequestBody ProviderRequestDTO request) {
        return providerManager.createMyProfile(userId, request);
    }

    @PutMapping("/me")
    public ProviderResponseDTO updateMyProfile(@CurrentUserId Long userId,
                                               @Valid @RequestBody ProviderRequestDTO request) {
        return providerManager.updateMyProfile(userId, request);
    }

    @GetMapping("/me/services")
    public List<ProviderServiceResponseDTO> myServices(@CurrentUserId Long userId) {
        return providerManager.listMyServices(userId);
    }

    @PostMapping("/me/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderServiceResponseDTO addMyService(@CurrentUserId Long userId,
                                                   @Valid @RequestBody ProviderServiceRequestDTO request) {
        return providerManager.addMyService(userId, request);
    }

    @DeleteMapping("/me/services/{providerServiceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMyService(@CurrentUserId Long userId, @PathVariable Long providerServiceId) {
        providerManager.removeMyService(userId, providerServiceId);
    }

    @GetMapping("/me/availability")
    public List<AvailabilityResponseDTO> myAvailability(@CurrentUserId Long userId) {
        return providerManager.listAvailability(providerManager.getMyProfile(userId).id());
    }

    @PutMapping("/me/availability")
    public List<AvailabilityResponseDTO> replaceMyAvailability(
            @CurrentUserId Long userId,
            @Valid @RequestBody List<AvailabilityRequestDTO> windows) {
        return providerManager.replaceMyAvailability(userId, windows);
    }

    @GetMapping("/me/earnings")
    public Map<String, Object> earnings(@CurrentUserId Long userId) {
        return providerManager.earningsSummary(userId);
    }
}
