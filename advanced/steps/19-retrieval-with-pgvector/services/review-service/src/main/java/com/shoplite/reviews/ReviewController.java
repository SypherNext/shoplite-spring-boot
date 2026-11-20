package com.shoplite.reviews;

import java.time.Duration;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** The same annotations as Spring MVC; the return types say "later" instead of "now". */
@RestController
class ReviewController {

    private final ReviewService reviews;
    private final ReviewEvents events;

    ReviewController(ReviewService reviews, ReviewEvents events) {
        this.reviews = reviews;
        this.events = events;
    }

    @GetMapping("/api/products/{productId}/reviews")
    Flux<ReviewResponse> forProduct(@PathVariable Long productId) {
        return reviews.forProduct(productId);
    }

    @GetMapping("/api/products/{productId}/reviews/summary")
    Mono<ReviewSummary> summary(@PathVariable Long productId) {
        return reviews.summary(productId);
    }

    @PostMapping("/api/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ReviewResponse> add(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt author) {
        return reviews.add(productId, request, author);
    }

    /**
     * Server-Sent Events: the connection stays open and every new review is pushed as an event.
     * A comment right away opens the stream for the client (the response is only sent once there is
     * something to send), and another every 15 seconds keeps proxies from closing an idle connection.
     */
    @GetMapping(path = "/api/reviews/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<ReviewResponse>> stream() {
        Flux<ServerSentEvent<ReviewResponse>> reviewsAsEvents = events.stream()
                .map(review -> ServerSentEvent.builder(review).event("review").id(review.id().toString()).build());
        Flux<ServerSentEvent<ReviewResponse>> keepAlive = Flux.interval(Duration.ZERO, Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<ReviewResponse>builder().comment("keep-alive").build());
        return Flux.merge(reviewsAsEvents, keepAlive);
    }

}
