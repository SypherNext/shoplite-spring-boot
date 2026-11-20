package com.shoplite.catalog;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reserves stock for a whole order in ONE catalog transaction, at most once per reservation id. */
@Service
class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final ProductService productService;
    private final StockReservations reservations;

    StockReservationService(ProductService productService, StockReservations reservations) {
        this.productService = productService;
        this.reservations = reservations;
    }

    /** Every line, or none. The same id a second time returns the first answer and takes nothing. */
    @Transactional
    List<ReservedProduct> reserve(UUID reservationId, List<StockLine> lines) {
        return reservations.productsOf(reservationId).orElseGet(() -> {
            List<ReservedProduct> reserved = lines.stream()
                    .map(line -> productService.reserveStock(line.productId(), line.quantity()))
                    .toList();
            reservations.save(reservationId, lines, reserved);
            log.info("Reserved stock {} for {} line(s): {}", reservationId, lines.size(), lines);
            return reserved;
        });
    }

    /** Puts the reserved stock back, once. Unknown or already released ids change nothing. */
    @Transactional
    void release(UUID reservationId) {
        reservations.remove(reservationId).ifPresent(lines -> {
            productService.releaseStock(lines);
            log.info("Released reservation {}: {}", reservationId, lines);
        });
    }

}
