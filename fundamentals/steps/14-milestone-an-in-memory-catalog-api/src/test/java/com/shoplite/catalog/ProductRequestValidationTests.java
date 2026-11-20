package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** Checks the rules on ProductRequest directly with a Validator, without starting Spring. */
class ProductRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAValidRequest() {
        ProductRequest request = new ProductRequest("Mug", "MUG-1", null, new BigDecimal("8.00"), 10, null, 1L);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reportsEveryBrokenRule() {
        ProductRequest request = new ProductRequest("", "bad sku", null, new BigDecimal("-1"), null, null, 1L);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "sku", "price", "stock");
    }

}
