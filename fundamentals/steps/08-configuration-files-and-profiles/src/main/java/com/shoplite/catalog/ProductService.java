package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final int lowStockThreshold;

    public ProductService(ProductRepository productRepository,
            @Value("${shoplite.shop.low-stock-threshold}") int lowStockThreshold) {
        this.productRepository = productRepository;
        this.lowStockThreshold = lowStockThreshold;
    }

    /** All products, sorted by name. */
    public List<Product> findAll() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getName))
                .toList();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    /** Active products whose stock is at or below the threshold, lowest stock first. */
    public List<Product> findLowStock() {
        return productRepository.findAll().stream()
                .filter(product -> product.isActive() && product.getStock() <= lowStockThreshold)
                .sorted(Comparator.comparing(Product::getStock).thenComparing(Product::getName))
                .toList();
    }

}
