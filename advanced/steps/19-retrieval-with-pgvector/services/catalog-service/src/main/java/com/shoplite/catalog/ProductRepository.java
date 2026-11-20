package com.shoplite.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);

    // DTO projection plus paging: one SELECT for the rows, one COUNT for the total.
    // Spring Data adds the ORDER BY and LIMIT/OFFSET from the Pageable.
    @Query(value = """
            select new com.shoplite.catalog.ProductSummary(p.id, p.name, p.sku, p.price, p.stock, c.name)
            from Product p join p.category c
            where (:categoryId is null or c.id = :categoryId)
              and lower(p.name) like lower(concat('%', :search, '%'))
            """,
            countQuery = """
            select count(p) from Product p
            where (:categoryId is null or p.category.id = :categoryId)
              and lower(p.name) like lower(concat('%', :search, '%'))
            """)
    Page<ProductSummary> search(@Param("categoryId") Long categoryId, @Param("search") String search,
            Pageable pageable);

    // Fetch plan with JOIN FETCH in JPQL: same effect as the entity graph above.
    @Query("""
            select p from Product p join fetch p.category
            where p.active = true and p.stock <= :threshold
            order by p.stock asc, p.name asc
            """)
    List<Product> findActiveWithStockAtOrBelow(@Param("threshold") int threshold);

}
