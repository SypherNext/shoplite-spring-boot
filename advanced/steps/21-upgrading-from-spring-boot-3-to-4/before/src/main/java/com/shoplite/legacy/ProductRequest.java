package com.shoplite.legacy;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(@NotBlank String name, @NotNull @DecimalMin("0.01") BigDecimal price) {
}
