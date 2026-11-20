package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

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

    /** All products, sorted by name. */
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getName))
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
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
        Category category = categoryService.getCategory(request.categoryId());
        Product product = new Product(request.name(), request.sku(), request.description(),
                request.price(), request.stock(), category);
        product.setActive(request.activeOrDefault());
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
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
