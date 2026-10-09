package com.fixora.service;

import com.fixora.dto.request.AvailabilityRequestDTO;
import com.fixora.dto.request.ProviderRequestDTO;
import com.fixora.dto.request.ProviderServiceRequestDTO;
import com.fixora.dto.response.*;
import com.fixora.enums.ProviderStatus;

import java.util.List;
import java.util.Map;

/** Provider onboarding, offerings, availability, verification and earnings. */
public interface ProviderManager {

    ProviderResponseDTO createMyProfile(Long userId, ProviderRequestDTO request);

    ProviderResponseDTO updateMyProfile(Long userId, ProviderRequestDTO request);

    ProviderResponseDTO getMyProfile(Long userId);

    ProviderResponseDTO getProvider(Long providerId);

    PageResponseDTO<ProviderResponseDTO> searchProviders(String city,
                                                         String area,
                                                         String keyword,
                                                         Long serviceId,
                                                         int page,
                                                         int size);

    PageResponseDTO<ProviderResponseDTO> listProviders(ProviderStatus status, int page, int size);

    ProviderResponseDTO updateVerification(Long providerId, ProviderStatus status, String note);

    List<ProviderServiceResponseDTO> listProviderServices(Long providerId);

    List<ProviderServiceResponseDTO> listMyServices(Long userId);

    ProviderServiceResponseDTO addMyService(Long userId, ProviderServiceRequestDTO request);

    void removeMyService(Long userId, Long providerServiceId);

    List<AvailabilityResponseDTO> listAvailability(Long providerId);

    List<AvailabilityResponseDTO> replaceMyAvailability(Long userId, List<AvailabilityRequestDTO> windows);

    Map<String, Object> earningsSummary(Long userId);
}
