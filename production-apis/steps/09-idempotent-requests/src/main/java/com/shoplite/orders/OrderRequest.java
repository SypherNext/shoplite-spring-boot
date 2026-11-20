package com.shoplite.orders;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** What a customer sends to place an order: one or more lines, at most 20. */
public record OrderRequest(
        @NotEmpty @Size(max = 20) List<@Valid OrderLineRequest> lines) {

    /** A stable text form of the request, used to spot a reused Idempotency-Key with a different body. */
    String fingerprint() {
        return lines.stream()
                .map(line -> line.productId() + "x" + line.quantity())
                .collect(Collectors.joining(","));
    }

}
