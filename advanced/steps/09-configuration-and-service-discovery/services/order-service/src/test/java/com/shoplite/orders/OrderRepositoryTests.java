package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.shoplite.TestcontainersConfiguration;
import com.shoplite.customers.Customer;

/**
 * A data slice test: only JPA, Flyway and the repositories start, against a real PostgreSQL
 * container. Each test runs in a transaction that is rolled back at the end.
 * Hibernate statistics count the SQL statements, which makes the N+1 problem visible.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // use the container, not H2
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTests {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TestEntityManager entityManager;

    Statistics statistics;

    @BeforeEach
    void createThreePaidOrdersForThreeCustomers() {
        ReservedProduct mug = new ReservedProduct(1L, "MUG-ENAMEL-01", "Enamel Mug", new BigDecimal("12.50"));
        for (String name : new String[] {"ann", "ben", "cat"}) {
            Customer customer = entityManager.persist(new Customer("sub-" + name, name + "@shoplite.test", name));
            Order order = new Order(customer, "EUR", null);
            order.addLine(mug, 1);
            order.markPaid("pay_" + name);
            entityManager.persist(order);
        }
        // Empty the persistence context, so the queries below really hit the database.
        entityManager.flush();
        entityManager.clear();
        statistics = entityManager.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void naiveQueryRunsOneExtraSelectPerCustomer() {
        Page<Order> page = orderRepository.findByStatus(OrderStatus.PAID, PageRequest.of(0, 10));
        page.forEach(order -> order.getCustomer().getEmail());

        // 1 query for the orders + 1 query for each of the 3 customers = 4: the N+1 problem.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    void joinFetchLoadsOrdersAndCustomersInOneQuery() {
        Page<Order> page = orderRepository.findByStatusWithCustomer(OrderStatus.PAID, PageRequest.of(0, 10));
        page.forEach(order -> order.getCustomer().getEmail());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void entityGraphLoadsAnOrderWithCustomerAndLinesInOneQuery() {
        Long orderId = orderRepository.findAll().getFirst().getId();
        statistics.clear();

        Order order = orderRepository.findWithCustomerAndLinesById(orderId).orElseThrow();

        assertThat(order.getCustomer().getEmail()).endsWith("@shoplite.test");
        assertThat(order.getLines()).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void projectionPagesReturnOnlySummaryColumns() {
        Long customerId = orderRepository.findAll().getFirst().getCustomer().getId();

        Page<OrderSummary> page = orderRepository.findByCustomerId(customerId,
                PageRequest.of(0, 5, Sort.by("createdAt").descending()));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getTotal()).isEqualByComparingTo("12.50");
        assertThat(page.getContent().getFirst().getStatus()).isEqualTo(OrderStatus.PAID);
    }

}
