package com.shoplite.reviews;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Every method builds a pipeline and returns it. Nothing runs until WebFlux subscribes, and no
 * thread waits while the catalog or the database works.
 */
@Service
class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository repository;
    private final CatalogLookup catalog;
    private final ReviewEvents events;

    ReviewService(ReviewRepository repository, CatalogLookup catalog, ReviewEvents events) {
        this.repository = repository;
        this.catalog = catalog;
        this.events = events;
    }

    Flux<ReviewResponse> forProduct(Long productId) {
        return repository.findByProductIdOrderByCreatedAtDesc(productId).map(ReviewResponse::from);
    }

    Mono<ReviewSummary> summary(Long productId) {
        return repository.summarize(productId);
    }

    Mono<ReviewResponse> add(Long productId, ReviewRequest request, Jwt author) {
        return catalog.existingProduct(productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product " + productId + " was not found")))
                .flatMap(id -> repository.save(new Review(null, id, author.getSubject(), displayName(author),
                        request.rating(), request.comment(), Instant.now())))
                .map(ReviewResponse::from)
                .doOnNext(review -> {
                    log.info("Review {} saved for product {} on {}", review.id(), productId,
                            Thread.currentThread().getName());
                    events.published(review);
                });
    }

    private static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : jwt.getClaimAsString("preferred_username");
    }

}
