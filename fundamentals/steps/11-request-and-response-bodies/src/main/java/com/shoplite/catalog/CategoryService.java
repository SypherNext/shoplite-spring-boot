package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shoplite.common.NotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** All categories, sorted by name. */
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getCategory(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        Category saved = categoryRepository.save(new Category(request.name(), request.description()));
        return CategoryResponse.from(saved);
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        category.setName(request.name());
        category.setDescription(request.description());
        // The repository holds this very object, so changing it is enough for now.
        return CategoryResponse.from(category);
    }

    public void delete(Long id) {
        categoryRepository.delete(getCategory(id));
    }

    /** Used by ProductService to attach a product to its category. */
    Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }

}
