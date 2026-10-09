package com.fixora.mapper;

import com.fixora.dto.response.RatingSummaryDTO;
import com.fixora.dto.response.ReviewResponseDTO;
import com.fixora.entity.Review;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ReviewMapper {

    public ReviewResponseDTO toDto(Review review) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getBooking() != null ? review.getBooking().getId() : null,
                review.getService() != null ? review.getService().getId() : null,
                review.getService() != null ? review.getService().getName() : null,
                review.getProvider() != null ? review.getProvider().getId() : null,
                review.getProvider() != null ? review.getProvider().getBusinessName() : null,
                review.getCustomer() != null ? review.getCustomer().getId() : null,
                review.getCustomer() != null ? review.getCustomer().getFullName() : null,
                review.getRating(),
                review.getComment(),
                review.getVisible(),
                review.getCreatedAt()
        );
    }

    /** Built purely from database aggregates. Never invents a rating. */
    public RatingSummaryDTO toSummary(Double average, long count) {
        if (average == null || count == 0) {
            return new RatingSummaryDTO(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0, false);
        }
        return new RatingSummaryDTO(
                BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP),
                count,
                true
        );
    }
}
