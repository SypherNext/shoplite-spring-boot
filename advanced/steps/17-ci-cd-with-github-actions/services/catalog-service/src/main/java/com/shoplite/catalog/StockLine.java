package com.shoplite.catalog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** A product and a quantity, as the catalog sees an order line. */
public record StockLine(@NotNull Long productId, @Positive int quantity) {
}
