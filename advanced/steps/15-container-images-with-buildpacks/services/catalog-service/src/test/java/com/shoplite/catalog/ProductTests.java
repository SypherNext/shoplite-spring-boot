package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.shoplite.common.ConflictException;

/** Plain unit tests for the stock rules on the Product entity. */
class ProductTests {

    private final Product bottle = new Product("Bottle", "BOTTLE-01", null, new BigDecimal("24.90"), 3,
            new Category("Kitchen", null));

    @Test
    void stockNeverGoesBelowZero() {
        assertThatThrownBy(() -> bottle.removeStock(4))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Only 3 of BOTTLE-01 left in stock");
        bottle.removeStock(3);
        assertThat(bottle.getStock()).isZero();
    }

}
