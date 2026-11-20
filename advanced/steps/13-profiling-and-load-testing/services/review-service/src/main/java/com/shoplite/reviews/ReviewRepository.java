package com.shoplite.reviews;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Like a JPA repository, but every method returns a Mono (0 or 1 item) or a Flux (0 to n items). */
public interface ReviewRepository extends ReactiveCrudRepository<Review, Long> {

    Flux<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("select count(*) as reviews, coalesce(avg(rating), 0) as average from reviews where product_id = :productId")
    Mono<ReviewSummary> summarize(Long productId);

}
