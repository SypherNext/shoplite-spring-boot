package com.shoplite.reviews;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Pushes every new review to whoever is listening on /api/reviews/stream right now.
 * A multicast sink sends each item to all current subscribers; "directBestEffort" drops an
 * item for a subscriber that is too slow instead of holding everyone else back.
 * In memory only: with several instances, each instance only streams its own new reviews.
 */
@Component
class ReviewEvents {

    private final Sinks.Many<ReviewResponse> sink = Sinks.many().multicast().directBestEffort();

    void published(ReviewResponse review) {
        sink.tryEmitNext(review);
    }

    Flux<ReviewResponse> stream() {
        return sink.asFlux();
    }

}
