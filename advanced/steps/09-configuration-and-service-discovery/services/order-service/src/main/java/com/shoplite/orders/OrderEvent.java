package com.shoplite.orders;

import java.util.List;

/**
 * Something that happened to an order. OrderService publishes these; other parts of the
 * app (notifications, metrics, later the catalog) react to them without OrderService
 * knowing who they are.
 *
 * Sealed: these three records are the only kinds of order event, so a switch over
 * OrderEvent must handle all of them and the compiler checks that it does.
 */
public sealed interface OrderEvent permits OrderPaid, OrderPaymentFailed, OrderCancelled {

    Long orderId();

    List<Line> lines();

    /** One line of the order: which product and how many. */
    record Line(Long productId, int quantity) {
    }

    static List<Line> linesOf(Order order) {
        return order.getLines().stream()
                .map(line -> new Line(line.getProductId(), line.getQuantity()))
                .toList();
    }

}
