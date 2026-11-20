package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shoplite.shop.ShopProperties;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopProperties shopProperties;

    public ProductService(ProductRepository productRepository, ShopProperties shopProperties) {
        this.productRepository = productRepository;
        this.shopProperties = shopProperties;
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

    /** Active products whose stock is at or below the threshold in the shop settings. */
    public List<Product> findLowStock() {
        int threshold = shopProperties.lowStockThreshold();
        return productRepository.findAll().stream()
                .filter(product -> product.isActive() && product.getStock() <= threshold)
                .sorted(Comparator.comparing(Product::getStock).thenComparing(Product::getName))
                .toList();
    }

}
