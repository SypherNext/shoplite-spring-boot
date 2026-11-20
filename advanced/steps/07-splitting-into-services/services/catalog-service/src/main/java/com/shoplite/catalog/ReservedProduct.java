package com.shoplite.catalog;

import java.math.BigDecimal;

/**
 * What the catalog tells the orders module about a product it just reserved: a copy of the
 * values, not the Product entity. Orders can no longer reach into the catalog's tables
 * through a JPA relationship, which is what makes the two modules separable later.
 */
public record ReservedProduct(Long id, String sku, String name, BigDecimal price) {

    static ReservedProduct from(Product product) {
        return new ReservedProduct(product.getId(), product.getSku(), product.getName(), product.getPrice());
    }

}
