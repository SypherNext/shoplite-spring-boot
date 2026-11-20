package com.shoplite.orders;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

/**
 * The catalog service's internal stock API, as an HTTP service client. Spring writes the
 * implementation; spring.http.serviceclient.catalog.* sets its address and timeouts.
 */
public interface CatalogStockClient {

    @PostExchange("/internal/stock/reservations")
    List<ReservedProduct> reserve(@RequestBody Reservation reservation);

    @PostExchange("/internal/stock/reservations/{reservationId}/release")
    void release(@PathVariable UUID reservationId);

    record Reservation(UUID reservationId, List<OrderLineRequest> lines) {
    }

}
