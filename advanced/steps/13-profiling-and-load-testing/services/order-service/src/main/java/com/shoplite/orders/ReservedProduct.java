package com.shoplite.orders;

import java.math.BigDecimal;

/** The catalog's answer for one reserved line: the product as it was at that moment. */
public record ReservedProduct(Long id, String sku, String name, BigDecimal price) {
}
