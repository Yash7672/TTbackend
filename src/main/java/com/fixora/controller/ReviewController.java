package com.fixora.controller;

import com.fixora.dto.request.ReviewRequestDTO;
import com.fixora.dto.response.ReviewResponseDTO;
import com.fixora.service.ReviewService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Reviews. A review is only accepted for the caller's own COMPLETED booking, and
 * the rating aggregates are recomputed from the real rows afterwards.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponseDTO create(@CurrentUserId Long customerId,
                                    @Valid @RequestBody ReviewRequestDTO request) {
        return reviewService.createReview(customerId, request);
    }
}
