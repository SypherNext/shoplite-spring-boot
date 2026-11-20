package com.shoplite.orders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The JSON that goes onto the "shoplite.order-events" topic. This is a public contract:
 * other teams and services read it, so fields are only ever added, never renamed or removed.
 */
public record OrderEventMessage(UUID eventId, String type, Long orderId, Instant occurredAt, List<Line> lines) {

    public static final String TOPIC = "shoplite.order-events";

    public record Line(Long productId, int quantity) {
    }

    /** Turns an in-process event into the message. The switch must cover every kind of OrderEvent. */
    static OrderEventMessage from(OrderEvent event) {
        String type = switch (event) {
            case OrderPaid paid -> "PAID";
            case OrderPaymentFailed failed -> "PAYMENT_FAILED";
            case OrderCancelled cancelled -> "CANCELLED";
        };
        List<Line> lines = event.lines().stream().map(line -> new Line(line.productId(), line.quantity())).toList();
        return new OrderEventMessage(UUID.randomUUID(), type, event.orderId(), Instant.now(), lines);
    }

}
