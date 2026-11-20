package com.shoplite.assistant;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/** The catalog's public product API, as an HTTP service client (group "catalog"). */
public interface CatalogClient {

    @GetExchange("/api/products")
    ProductPage search(@RequestParam String search, @RequestParam int size);

    @GetExchange("/api/products/{id}")
    ProductDetails product(@PathVariable long id);

    /** One page of the catalog's product list. Only the fields the assistant needs are read. */
    record ProductPage(List<ProductHit> content) {
    }

    record ProductHit(long id, String name, BigDecimal price, int stock, String categoryName) {
    }

    record ProductDetails(long id, String name, String description, BigDecimal price, int stock,
            boolean active, String categoryName) {
    }

}
