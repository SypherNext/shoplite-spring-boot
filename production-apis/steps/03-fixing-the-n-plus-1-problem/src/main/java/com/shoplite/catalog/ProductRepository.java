package com.shoplite.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Fetch plan with an entity graph: load the category in the same SQL (a join),
    // instead of one extra SELECT per product later (the N+1 problem).
    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySku(String sku);

    // Derived queries: Spring Data reads the method name and writes the SQL.
    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String text);

    List<Product> findAllByOrderByNameAsc();

    // Fetch plan with JOIN FETCH in JPQL: same effect as the entity graph above.
    @Query("""
            select p from Product p join fetch p.category
            where p.active = true and p.stock <= :threshold
            order by p.stock asc, p.name asc
            """)
    List<Product> findActiveWithStockAtOrBelow(@Param("threshold") int threshold);

}
