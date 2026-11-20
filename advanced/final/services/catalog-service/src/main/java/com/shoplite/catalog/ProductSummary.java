package com.shoplite.catalog;

import java.math.BigDecimal;

/**
 * A DTO projection for product lists: only the columns a list needs,
 * filled straight from the query (see ProductRepository.search), with no entities loaded.
 */
public record ProductSummary(Long id, String name, String sku, BigDecimal price, int stock, String categoryName) {
}
