package com.shoplite.catalog;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for other ShopLite services, not for shop customers. The API gateway (next lesson)
 * never routes /internal/**, and the OpenAPI document leaves it out.
 */
@RestController
@RequestMapping("/internal/stock/reservations")
class InternalStockController {

    private final StockReservationService reservations;

    InternalStockController(StockReservationService reservations) {
        this.reservations = reservations;
    }

    /** 200 with one ReservedProduct per line; 409 if any line cannot be served (nothing is reserved then). */
    @PostMapping
    List<ReservedProduct> reserve(@Valid @RequestBody StockReservationRequest request) {
        return reservations.reserve(request.reservationId(), request.lines());
    }

    /** 204 whether the stock went back now, earlier, or the reservation never existed. */
    @PostMapping("/{reservationId}/release")
    ResponseEntity<Void> release(@PathVariable UUID reservationId) {
        reservations.release(reservationId);
        return ResponseEntity.noContent().build();
    }

}
