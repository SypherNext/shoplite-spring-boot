package com.shoplite.catalog;

import java.util.List;
import java.util.UUID;

/**
 * The catalog's own view of a message on "shoplite.order-events". It names only the fields the
 * catalog needs; Jackson ignores the rest. The catalog does not import any class from orders,
 * so the two can change (and later deploy) independently. The JSON is the contract.
 */
record OrderStockMessage(UUID eventId, String type, Long orderId, List<StockLine> lines) {

    static final String TOPIC = "shoplite.order-events";

    /** Order events after which the reserved stock goes back on the shelf. */
    boolean releasesStock() {
        return "PAYMENT_FAILED".equals(type) || "CANCELLED".equals(type);
    }

}
