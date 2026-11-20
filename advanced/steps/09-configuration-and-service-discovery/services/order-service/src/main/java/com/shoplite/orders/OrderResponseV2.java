package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Version 2.0 of an order. Breaking changes compared with 1.0: the total is a Money object,
 * createdAt is renamed placedAt, and there is a new itemCount field.
 * Old clients keep getting 1.0 until they send API-Version: 2.0.
 */
public record OrderResponseV2(
        Long id,
        OrderStatus status,
        Money total,
        Instant placedAt,
        int itemCount,
        long version,
        List<OrderLineResponse> lines) {

    public record Money(BigDecimal amount, String currency) {
    }

    static OrderResponseV2 from(OrderDetails order) {
        return new OrderResponseV2(order.id(), order.status(), new Money(order.total(), order.currency()),
                order.createdAt(), order.itemCount(), order.version(), order.lines());
    }

}
