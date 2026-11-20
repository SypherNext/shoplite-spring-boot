package com.shoplite.orders;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;

/**
 * The order service's view of the catalog: reserve stock for an order, or give it back.
 * Translates the catalog's HTTP answers into this service's own exceptions, so the rest of
 * the code never sees HTTP.
 */
@Component
public class CatalogStock {

    private static final Logger log = LoggerFactory.getLogger(CatalogStock.class);

    private final CatalogStockClient client;

    public CatalogStock(CatalogStockClient client) {
        this.client = client;
    }

    /**
     * One catalog transaction: every line is reserved, or none. Calling it again with the same
     * reservation id returns the first answer, so a retry after a timeout never reserves twice.
     */
    public List<ReservedProduct> reserve(UUID reservationId, List<OrderLineRequest> lines) {
        try {
            return client.reserve(new CatalogStockClient.Reservation(reservationId, lines));
        } catch (HttpClientErrorException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            log.warn("Catalog reservation {} failed: {}", reservationId, ex.getMessage());
            throw new CatalogUnavailableException(ex);
        }
    }

    /** Compensation: puts reserved stock back when the order could not be saved after all. */
    public void release(UUID reservationId) {
        try {
            client.release(reservationId);
            log.info("Released reservation {} after a failed order", reservationId);
        } catch (RestClientException ex) {
            // The stock stays reserved. A real shop would retry this release later (an outbox again).
            log.error("Could not release reservation {}: {}", reservationId, ex.getMessage());
        }
    }

    private static RuntimeException translate(HttpClientErrorException ex) {
        ProblemDetail problem = ex.getResponseBodyAs(ProblemDetail.class);
        String detail = problem != null && problem.getDetail() != null ? problem.getDetail() : ex.getMessage();
        if (ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return new NotFoundException(detail);
        }
        if (ex.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
            return new ConflictException(detail);
        }
        return new CatalogUnavailableException(ex);
    }

}
