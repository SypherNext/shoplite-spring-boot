package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Everything the API can say about one order, copied out of the entities while the
 * transaction is still open. Controllers turn it into a v1 or v2 response.
 */
public record OrderDetails(
        Long id,
        OrderStatus status,
        BigDecimal total,
        String currency,
        Instant createdAt,
        String customerEmail,
        String notifyUrl,
        long version,
        List<OrderLineResponse> lines) {

    static OrderDetails from(Order order) {
        return new OrderDetails(
                order.getId(),
                order.getStatus(),
                order.getTotal(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getCustomer().getEmail(),
                order.getNotifyUrl(),
                order.getVersion(),
                order.getLines().stream().map(OrderLineResponse::from).toList());
    }

    int itemCount() {
        return lines.stream().mapToInt(OrderLineResponse::quantity).sum();
    }

}
