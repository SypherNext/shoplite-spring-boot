package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of the admin order list. */
public record AdminOrderRow(Long id, OrderStatus status, BigDecimal total, String currency, String customerEmail,
        Instant createdAt) {

    static AdminOrderRow from(Order order) {
        return new AdminOrderRow(order.getId(), order.getStatus(), order.getTotal(), order.getCurrency(),
                order.getCustomer().getEmail(), order.getCreatedAt());
    }

}
