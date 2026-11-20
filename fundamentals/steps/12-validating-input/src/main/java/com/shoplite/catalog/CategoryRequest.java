package com.shoplite.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What a client sends to create or update a category. */
public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description) {
}
