package com.fixora.service;

import com.fixora.dto.request.ReviewRequestDTO;
import com.fixora.dto.response.PageResponseDTO;
import com.fixora.dto.response.RatingSummaryDTO;
import com.fixora.dto.response.ReviewResponseDTO;

public interface ReviewService {

    ReviewResponseDTO createReview(Long customerId, ReviewRequestDTO request);

    PageResponseDTO<ReviewResponseDTO> reviewsForService(Long serviceId, int page, int size);

    PageResponseDTO<ReviewResponseDTO> reviewsForProvider(Long providerId, int page, int size);

    RatingSummaryDTO serviceSummary(Long serviceId);

    RatingSummaryDTO providerSummary(Long providerId);

    PageResponseDTO<ReviewResponseDTO> allReviews(int page, int size);

    ReviewResponseDTO setVisibility(Long reviewId, boolean visible);
}
