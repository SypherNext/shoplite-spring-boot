package com.shoplite.catalog;

import java.math.BigDecimal;

/** What the API returns for a product. */
public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        BigDecimal price,
        int stock,
        boolean active,
        Long categoryId,
        String categoryName) {

    static ProductResponse from(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.isActive(),
                category.getId(),
                category.getName());
    }

}
