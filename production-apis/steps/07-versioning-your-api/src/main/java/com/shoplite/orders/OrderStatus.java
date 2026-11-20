package com.shoplite.orders;

import java.util.Set;

/** The life of an order, and which moves an admin is allowed to make. */
public enum OrderStatus {

    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    SHIPPED,
    CANCELLED;

    /** Status changes an admin may make by hand. Payment results are set by the checkout, not by admins. */
    public boolean canMoveTo(OrderStatus next) {
        return this == PAID && Set.of(SHIPPED, CANCELLED).contains(next);
    }

}
