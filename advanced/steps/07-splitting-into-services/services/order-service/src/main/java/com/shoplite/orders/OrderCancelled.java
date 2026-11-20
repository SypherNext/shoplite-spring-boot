package com.shoplite.orders;

import java.util.List;

/** An admin cancelled a paid order. Its stock goes back on the shelf. */
public record OrderCancelled(Long orderId, List<Line> lines) implements OrderEvent {

    static OrderCancelled of(Order order) {
        return new OrderCancelled(order.getId(), OrderEvent.linesOf(order));
    }

}
