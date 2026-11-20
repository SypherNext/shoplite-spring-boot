package com.shoplite.orders;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Entity graph: the order, its customer and its lines in one SQL statement.
    @EntityGraph(attributePaths = {"customer", "lines"})
    Optional<Order> findWithCustomerAndLinesById(Long id);

    // Interface projection: Spring Data selects only the columns OrderSummary asks for.
    Page<OrderSummary> findByCustomerId(Long customerId, Pageable pageable);

    // JOIN FETCH brings each order's customer along, so the admin list does not run
    // one extra query per row. A paged fetch join needs its own count query.
    @Query(value = "select o from Order o join fetch o.customer where o.status = :status",
            countQuery = "select count(o) from Order o where o.status = :status")
    Page<Order> findByStatusWithCustomer(@Param("status") OrderStatus status, Pageable pageable);

    // The naive version, kept to show the N+1 problem in OrderRepositoryTests:
    // reading order.getCustomer().getEmail() on each row runs one SELECT per customer.
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

}
