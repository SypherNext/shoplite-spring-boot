package com.shoplite.orders;

import java.math.BigDecimal;
import java.util.List;

/** The payment provider approved the charge. */
public record OrderPaid(Long orderId, Long customerId, BigDecimal total, String currency, String notifyUrl,
        List<Line> lines) implements OrderEvent {

    static OrderPaid of(Order order) {
        return new OrderPaid(order.getId(), order.getCustomer().getId(), order.getTotal(), order.getCurrency(),
                order.getNotifyUrl(), OrderEvent.linesOf(order));
    }

}
