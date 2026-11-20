package com.shoplite.orders;

/** The order a checkout produced, and whether it was replayed from an earlier request with the same key. */
public record CheckoutResult(OrderDetails order, boolean replayed) {
}
