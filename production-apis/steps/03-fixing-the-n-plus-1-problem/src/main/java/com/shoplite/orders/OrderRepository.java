package com.shoplite.orders;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Entity graph: the order, its customer and its lines in one SQL statement.
    @EntityGraph(attributePaths = {"customer", "lines"})
    Optional<Order> findWithCustomerAndLinesById(Long id);

    // "CustomerId" walks the relationship (order.customer.id). The id is already in the
    // orders.customer_id column, so the SQL needs no join.
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // JOIN FETCH brings each order's customer along, so the admin list does not run
    // one extra query per customer.
    @Query("select o from Order o join fetch o.customer where o.status = :status order by o.createdAt")
    List<Order> findByStatusWithCustomer(@Param("status") OrderStatus status);

    // The naive version, kept to compare: reading order.getCustomer().getEmail()
    // on each row runs one SELECT per customer.
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

}
