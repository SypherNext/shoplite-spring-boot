package com.shoplite.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Keeps products in a map. Everything is lost when the app stops. */
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(nextId.getAndIncrement());
        }
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public void delete(Product product) {
        products.remove(product.getId());
    }

    @Override
    public boolean existsBySku(String sku) {
        return products.values().stream()
                .anyMatch(product -> product.getSku().equals(sku));
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return products.values().stream()
                .anyMatch(product -> product.getCategory().getId().equals(categoryId));
    }

}
