package com.shoplite.reviews;

import java.time.Instant;

public record ReviewResponse(Long id, Long productId, String authorName, int rating, String comment,
        Instant createdAt) {

    static ReviewResponse from(Review review) {
        return new ReviewResponse(review.id(), review.productId(), review.authorName(), review.rating(),
                review.comment(), review.createdAt());
    }

}
