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
    public List<Category> findAll() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .toList();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }

}
