package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.shoplite.catalog.Category;
import com.shoplite.catalog.Product;
import com.shoplite.catalog.ReservedProduct;
import com.shoplite.common.ConflictException;
import com.shoplite.customers.Customer;

/** Plain unit tests: no Spring, no database. Entities are ordinary Java objects. */
class OrderTests {

    private final Category kitchen = new Category("Kitchen", null);
    private final ReservedProduct mug = new ReservedProduct(1L, "MUG-ENAMEL-01", "Enamel Mug", new BigDecimal("12.50"));
    private final ReservedProduct bottle = new ReservedProduct(2L, "BOTTLE-01", "Bottle", new BigDecimal("24.90"));
    private final Product bottleInStock = new Product("Bottle", "BOTTLE-01", null, new BigDecimal("24.90"), 3, kitchen);

    @Test
    void totalIsTheSumOfItsLines() {
        Order order = newOrder();
        order.addLine(mug, 2);
        order.addLine(bottle, 1);

        assertThat(order.getTotal()).isEqualByComparingTo("49.90");
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void linesCopyTheProductAtTheMomentOfTheOrder() {
        Order order = newOrder();
        order.addLine(mug, 1);

        OrderLine line = order.getLines().getFirst();
        assertThat(line.getProductId()).isEqualTo(1L);
        assertThat(line.getProductSku()).isEqualTo("MUG-ENAMEL-01");
        assertThat(line.getUnitPrice()).isEqualByComparingTo("12.50");
    }

    @Test
    void onlyPaidOrdersCanBeShippedOrCancelled() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPED)).isInstanceOf(ConflictException.class);

        order.markPaid("pay_1");
        order.changeStatus(OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void stockNeverGoesBelowZero() {
        assertThatThrownBy(() -> bottleInStock.removeStock(4))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Only 3 of BOTTLE-01 left in stock");
        bottleInStock.removeStock(3);
        assertThat(bottleInStock.getStock()).isZero();
    }

    private static Order newOrder() {
        return new Order(new Customer("sub-1", "alice@shoplite.test", "Alice"), "EUR", null);
    }

}
