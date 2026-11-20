package com.shoplite.reviews;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Asks the catalog whether a product exists, without blocking a thread while it waits. */
@Component
class CatalogLookup {

    private final WebClient catalog;

    CatalogLookup(WebClient.Builder builder, @Value("${shoplite.catalog.base-url}") String baseUrl) {
        this.catalog = builder.baseUrl(baseUrl).build();
    }

    /** Completes empty when the product is unknown (404), with an error for anything else that fails. */
    Mono<Long> existingProduct(Long productId) {
        return catalog.get().uri("/api/products/{id}", productId)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.releaseBody().thenReturn(productId);
                    }
                    if (response.statusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return response.releaseBody().then(Mono.empty());
                    }
                    return response.createError();
                });
    }

}
