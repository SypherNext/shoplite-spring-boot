package com.shoplite.orders;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.shoplite.catalog.Product;

@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The owning side of the Order.lines relationship: this column holds the foreign key.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    // Copied from the product when the order is placed, so the order never changes afterwards.
    @Column(name = "product_sku", nullable = false, length = 40)
    private String productSku;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderLine() {
    }

    OrderLine(Order order, Product product, int quantity) {
        this.order = order;
        this.product = product;
        this.productSku = product.getSku();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    /** The product id without loading the product: Hibernate already knows it from the foreign key. */
    public Long getProductId() {
        return product.getId();
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

}
