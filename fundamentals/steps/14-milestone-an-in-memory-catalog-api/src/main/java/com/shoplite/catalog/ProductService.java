package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;
import com.shoplite.shop.ShopProperties;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ShopProperties shopProperties;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
            ShopProperties shopProperties) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.shopProperties = shopProperties;
    }

    /** Lists products, optionally filtered by category or by a piece of the name. */
    public List<ProductResponse> search(Long categoryId, String nameContains) {
        Stream<Product> products = productRepository.findAll().stream();
        if (categoryId != null) {
            products = products.filter(product -> product.getCategory().getId().equals(categoryId));
        } else if (nameContains != null && !nameContains.isBlank()) {
            String text = nameContains.strip().toLowerCase();
            products = products.filter(product -> product.getName().toLowerCase().contains(text));
        }
        return products
                .sorted(Comparator.comparing(Product::getName))
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product with SKU", sku));
    }

    /** Active products whose stock is at or below the threshold in the shop settings. */
    public List<ProductResponse> findLowStock() {
        int threshold = shopProperties.lowStockThreshold();
        return productRepository.findAll().stream()
                .filter(product -> product.isActive() && product.getStock() <= threshold)
                .sorted(Comparator.comparing(Product::getStock).thenComparing(Product::getName))
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException("A product with SKU " + request.sku() + " already exists");
        }
        Category category = categoryService.getCategory(request.categoryId());
        Product product = new Product(request.name(), request.sku(), request.description(),
                request.price(), request.stock(), category);
        product.setActive(request.activeOrDefault());
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        boolean skuChanged = !product.getSku().equals(request.sku());
        if (skuChanged && productRepository.existsBySku(request.sku())) {
            throw new ConflictException("A product with SKU " + request.sku() + " already exists");
        }
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setActive(request.activeOrDefault());
        product.setCategory(categoryService.getCategory(request.categoryId()));
        return ProductResponse.from(product);
    }

    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

}
