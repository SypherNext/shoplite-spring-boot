package com.shoplite.catalog;

import java.util.List;
import java.util.Optional;

/** Stores categories. The service depends on this interface, not on one way of storing them. */
public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(Long id);

    Category save(Category category);

    void delete(Category category);

}
