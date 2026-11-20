package com.shoplite.orders;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** What a customer sends to place an order: one or more lines, at most 20. */
public record OrderRequest(
        @NotEmpty @Size(max = 20) List<@Valid OrderLineRequest> lines) {
}
