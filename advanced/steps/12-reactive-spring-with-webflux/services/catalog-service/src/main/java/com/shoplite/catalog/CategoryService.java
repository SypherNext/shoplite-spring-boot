package com.shoplite.catalog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A category named '" + request.name() + "' already exists");
        }
        Category saved = categoryRepository.save(new Category(request.name(), request.description()));
        log.info("Created category {} '{}'", saved.getId(), saved.getName());
        return CategoryResponse.from(saved);
    }

    // Cached products carry their category name, so a rename clears the whole products cache.
    @CacheEvict(cacheNames = CatalogCacheConfig.PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        category.setName(request.name());
        category.setDescription(request.description());
        // No save() call: the entity is managed, so the change is written when the transaction commits.
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category " + id + " still has products. Move or delete them first");
        }
        categoryRepository.delete(category);
        log.info("Deleted category {}", id);
    }

    /** Used by ProductService to attach a product to its category. */
    Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }

}
