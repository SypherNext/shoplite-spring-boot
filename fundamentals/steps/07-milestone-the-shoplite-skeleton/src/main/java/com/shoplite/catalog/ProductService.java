package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

}
