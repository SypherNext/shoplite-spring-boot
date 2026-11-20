package com.shoplite.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public List<ProductResponse> search(Long categoryId, String nameContains) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdOrderByNameAsc(categoryId);
        } else if (nameContains != null && !nameContains.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCaseOrderByNameAsc(nameContains.strip());
        } else {
            products = productRepository.findAllByOrderByNameAsc();
        }
        return products.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product with SKU", sku));
    }

    /** Active products whose stock is at or below the threshold in the shop settings. */
    @Transactional(readOnly = true)
    public List<ProductResponse> findLowStock() {
        return productRepository.findActiveWithStockAtOrBelow(shopProperties.lowStockThreshold()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

}
