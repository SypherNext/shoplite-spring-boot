package com.shoplite.catalog;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.common.BadRequestException;
import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;
import com.shoplite.shop.ShopProperties;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    // Only these fields may appear in ?sort=. Anything else would reach the query and fail with a 500.
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "sku", "price", "stock");

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ShopProperties shopProperties;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
            ShopProperties shopProperties) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.shopProperties = shopProperties;
    }

    /** One page of products, optionally filtered by category and by a piece of the name. */
    @Transactional(readOnly = true)
    public Page<ProductSummary> search(Long categoryId, String nameContains, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'. Use one of " + SORTABLE_FIELDS);
            }
        }
        String search = nameContains == null ? "" : nameContains.strip();
        return productRepository.search(categoryId, search, pageable);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProductWithCategory(id));
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
        Product saved = productRepository.save(product);
        log.info("Created product {} with SKU {}", saved.getId(), saved.getSku());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProductWithCategory(id);
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
        productRepository.delete(getProductWithCategory(id));
        log.info("Deleted product {}", id);
    }

    /** Takes stock for an order line and returns the product so the order can copy its name and price. */
    @Transactional
    public Product reserveStock(Long productId, int quantity) {
        Product product = getProductWithCategory(productId);
        product.removeStock(quantity);
        return product;
    }

    private Product getProductWithCategory(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

}
