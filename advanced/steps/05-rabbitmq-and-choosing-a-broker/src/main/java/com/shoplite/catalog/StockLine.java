package com.shoplite.catalog;

/** A product and a quantity, as the catalog sees an order line. */
public record StockLine(Long productId, int quantity) {
}
