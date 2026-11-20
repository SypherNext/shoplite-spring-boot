package com.shoplite.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Derived query: Spring Data builds the SQL from the method name.
    List<Category> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

}
