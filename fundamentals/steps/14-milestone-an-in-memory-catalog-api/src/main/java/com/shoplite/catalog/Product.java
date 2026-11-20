package com.shoplite.catalog;

import java.math.BigDecimal;

/** Something the shop sells. Kept in memory for now. */
public class Product {

    private Long id;
    private String name;
    private String sku;
    private String description;
    // BigDecimal, never double, for money: 0.1 + 0.2 must equal 0.3.
    private BigDecimal price;
    private int stock;
    private boolean active = true;
    private Category category;

    public Product(String name, String sku, String description, BigDecimal price, int stock, Category category) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    // Only the repository sets the id, when it stores a new product.
    void setId(Long id) {
        this.id = id;
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
