package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** What the API returns for an order. */
public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal total,
        String currency,
        Instant createdAt,
        List<OrderLineResponse> lines) {

    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotal(), order.getCurrency(),
                order.getCreatedAt(), order.getLines().stream().map(OrderLineResponse::from).toList());
    }

}
