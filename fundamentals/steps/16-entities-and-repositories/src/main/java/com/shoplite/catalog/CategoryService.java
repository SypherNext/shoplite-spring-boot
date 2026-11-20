package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /** All categories, sorted by name. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
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
        return CategoryResponse.from(saved);
    }

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
    }

    /** Used by ProductService to attach a product to its category. */
    Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }

}
