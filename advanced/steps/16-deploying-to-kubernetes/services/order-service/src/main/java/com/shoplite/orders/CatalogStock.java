package com.shoplite.orders;

import java.util.List;
import java.util.UUID;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

/**
 * The order service's view of the catalog: reserve stock for an order, or give it back.
 * The circuit breaker protects every call, whichever transport (StockGateway) carries it.
 */
@Component
public class CatalogStock {

    private static final Logger log = LoggerFactory.getLogger(CatalogStock.class);

    private final StockGateway gateway;
    private final CircuitBreaker circuitBreaker;

    public CatalogStock(StockGateway gateway, CircuitBreakerFactory<?, ?> circuitBreakers) {
        this.gateway = gateway;
        // One breaker for every call to the catalog. Its settings: resilience4j.circuitbreaker.instances.catalog.*
        this.circuitBreaker = circuitBreakers.create("catalog");
    }

    /**
     * One catalog transaction: every line is reserved, or none. Calling it again with the same
     * reservation id returns the first answer, so a retry after a timeout never reserves twice.
     */
    public List<ReservedProduct> reserve(UUID reservationId, List<OrderLineRequest> lines) {
        return circuitBreaker.run(() -> gateway.reserve(reservationId, lines),
                failure -> afterFailure(reservationId, failure));
    }

    /**
     * Every failure arrives here. An open circuit (CallNotPermittedException) means the catalog was not
     * even called: fail fast with the usual 503. Anything else (a 409 from the catalog, a timeout) is
     * passed on unchanged; the breaker has already counted it or ignored it.
     */
    private List<ReservedProduct> afterFailure(UUID reservationId, Throwable failure) {
        if (failure instanceof CallNotPermittedException) {
            log.warn("Catalog circuit is open, reservation {} not attempted", reservationId);
            throw new CatalogUnavailableException(failure);
        }
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        throw new CatalogUnavailableException(failure);
    }

    /** Compensation: puts reserved stock back when the order could not be saved after all. */
    public void release(UUID reservationId) {
        try {
            gateway.release(reservationId);
            log.info("Released reservation {} after a failed order", reservationId);
        } catch (RuntimeException ex) {
            // The stock stays reserved. A real shop would retry this release later (an outbox again).
            log.error("Could not release reservation {}: {}", reservationId, ex.getMessage());
        }
    }

}
