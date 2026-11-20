package com.shoplite.orders;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // "CustomerId" walks the relationship (order.customer.id). The id is already in the
    // orders.customer_id column, so the SQL needs no join.
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

}
