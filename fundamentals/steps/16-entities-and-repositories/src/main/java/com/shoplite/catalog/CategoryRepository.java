package com.shoplite.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data writes the implementation of this interface at startup.
 * findAll, findById, save and delete come from JpaRepository.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

}
