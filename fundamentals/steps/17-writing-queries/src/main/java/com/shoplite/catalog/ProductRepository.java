package com.shoplite.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Derived queries: Spring Data reads the method name and writes the SQL.
    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String text);

    List<Product> findAllByOrderByNameAsc();

    // A JPQL query: it names entities and fields (Product, stock), not tables and columns.
    @Query("""
            select p from Product p
            where p.active = true and p.stock <= :threshold
            order by p.stock asc, p.name asc
            """)
    List<Product> findActiveWithStockAtOrBelow(@Param("threshold") int threshold);

}
