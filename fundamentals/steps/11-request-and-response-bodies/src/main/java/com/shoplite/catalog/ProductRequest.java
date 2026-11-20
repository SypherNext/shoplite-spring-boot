package com.shoplite.catalog;

import java.math.BigDecimal;

/** What a client sends to create or update a product. */
public record ProductRequest(
        String name,
        String sku,
        String description,
        BigDecimal price,
        Integer stock,
        Boolean active,
        Long categoryId) {

    /** A missing "active" field means the product is for sale. */
    boolean activeOrDefault() {
        return active == null || active;
    }

}
