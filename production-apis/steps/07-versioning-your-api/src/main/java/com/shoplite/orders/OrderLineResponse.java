package com.shoplite.orders;

import java.math.BigDecimal;

public record OrderLineResponse(Long productId, String sku, String name, BigDecimal unitPrice, int quantity,
        BigDecimal lineTotal) {

    static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getProductId(), line.getProductSku(), line.getProductName(),
                line.getUnitPrice(), line.getQuantity(), line.lineTotal());
    }

}
