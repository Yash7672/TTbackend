package com.fixora.dto.response;

import java.math.BigDecimal;

/**
 * Rating summary built only from real review rows. `hasReviews` is false until a
 * genuine review exists, so the UI shows "No reviews yet" instead of inventing one.
 */
public record RatingSummaryDTO(
        BigDecimal averageRating,
        long reviewCount,
        boolean hasReviews
) {
}
