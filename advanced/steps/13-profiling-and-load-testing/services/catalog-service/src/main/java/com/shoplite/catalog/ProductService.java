package com.shoplite.catalog;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final CacheManager cacheManager;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
            ShopProperties shopProperties, CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.shopProperties = shopProperties;
        this.cacheManager = cacheManager;
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

    // The first call reads PostgreSQL and stores the result in Redis under "products::<id>".
    // Later calls are answered from Redis until the entry expires or is evicted.
    @Cacheable(cacheNames = CatalogCacheConfig.PRODUCTS_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        log.debug("Cache miss for product {}, loading from the database", id);
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

    @CacheEvict(cacheNames = CatalogCacheConfig.PRODUCTS_CACHE, key = "#id")
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

    @CacheEvict(cacheNames = CatalogCacheConfig.PRODUCTS_CACHE, key = "#id")
    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProductWithCategory(id));
        log.info("Deleted product {}", id);
    }

    /**
     * Takes stock for an order line and returns the product so the order can copy its name and price.
     * MANDATORY: this must run inside the caller's transaction, so the stock change and the
     * order are saved together or not at all.
     */
    @CacheEvict(cacheNames = CatalogCacheConfig.PRODUCTS_CACHE, key = "#productId")
    @Transactional(propagation = Propagation.MANDATORY)
    public ReservedProduct reserveStock(Long productId, int quantity) {
        Product product = getProductWithCategory(productId);
        product.removeStock(quantity);
        return ReservedProduct.from(product);
    }

    /**
     * Puts stock back for every line, in one transaction. Called by the Kafka listener when an
     * order's payment fails or the order is cancelled.
     * A method calling another method of the same class skips the Spring proxy, so a @CacheEvict
     * on a per-product method would not run. The cache entries are evicted here instead (after
     * the commit, because the cache manager is transaction-aware).
     */
    @Transactional
    public void releaseStock(List<StockLine> lines) {
        Cache products = cacheManager.getCache(CatalogCacheConfig.PRODUCTS_CACHE);
        for (StockLine line : lines) {
            getProductWithCategory(line.productId()).addStock(line.quantity());
            products.evict(line.productId());
        }
    }

    private Product getProductWithCategory(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

}
