package com.shoplite.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed shop settings read from the {@code shoplite.shop.*} properties.
 * {@code @Validated} makes the app refuse to start when a value is missing or wrong.
 */
@Validated
@ConfigurationProperties("shoplite.shop")
public record ShopProperties(
        @NotBlank String name,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Min(0) int lowStockThreshold) {
}
