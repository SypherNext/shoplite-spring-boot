package com.shoplite.orders;

import java.util.List;

/** The payment provider declined the charge. The reserved stock must go back on the shelf. */
public record OrderPaymentFailed(Long orderId, List<Line> lines) implements OrderEvent {

    static OrderPaymentFailed of(Order order) {
        return new OrderPaymentFailed(order.getId(), OrderEvent.linesOf(order));
    }

}
