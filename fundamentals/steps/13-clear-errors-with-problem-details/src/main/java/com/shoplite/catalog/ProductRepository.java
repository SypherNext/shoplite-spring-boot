package com.shoplite.catalog;

import java.util.List;
import java.util.Optional;

/** Stores products. Same idea as CategoryRepository: the services only see this interface. */
public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    Product save(Product product);

    void delete(Product product);

    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);

}
