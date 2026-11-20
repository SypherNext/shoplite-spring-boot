package com.shoplite.catalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** What a client sends to create or update a product. */
public record ProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,40}", message = "must be 3 to 40 capital letters, digits or dashes")
        String sku,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock,
        Boolean active,
        @NotNull Long categoryId) {

    /** A missing "active" field means the product is for sale. */
    boolean activeOrDefault() {
        return active == null || active;
    }

}
