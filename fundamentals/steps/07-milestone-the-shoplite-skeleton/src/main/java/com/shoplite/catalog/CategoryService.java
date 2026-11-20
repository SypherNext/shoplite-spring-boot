package com.shoplite.catalog;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // One constructor: Spring calls it and passes in the CategoryRepository bean.
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** All categories, sorted by name. */
    public List<Category> findAll() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .toList();
    }

}
