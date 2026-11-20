package com.shoplite.catalog;

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

import com.shoplite.common.ConflictException;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(length = 2000)
    private String description;

    // BigDecimal, never double, for money: 0.1 + 0.2 must equal 0.3.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active = true;

    // LAZY: the category row is only loaded when the code asks for it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    protected Product() {
    }

    public Product(String name, String sku, String description, BigDecimal price, int stock, Category category) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    /** Takes items out of stock for an order. Refuses to go below zero. */
    public void removeStock(int quantity) {
        if (!active) {
            throw new ConflictException("Product " + sku + " is not for sale");
        }
        if (quantity > stock) {
            throw new ConflictException("Only " + stock + " of " + sku + " left in stock");
        }
        stock -= quantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

}
