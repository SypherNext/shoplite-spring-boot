package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Version 1.0 of an order in the API: the total is a plain number next to a currency code. */
public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal total,
        String currency,
        Instant createdAt,
        long version,
        List<OrderLineResponse> lines) {

    static OrderResponse from(OrderDetails order) {
        return new OrderResponse(order.id(), order.status(), order.total(), order.currency(),
                order.createdAt(), order.version(), order.lines());
    }

}
