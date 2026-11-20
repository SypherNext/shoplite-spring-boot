package com.shoplite.orders;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a customer sends to place an order.
 * notifyUrl is optional: ShopLite POSTs a small JSON message there once the order is paid.
 */
public record OrderRequest(
        @NotEmpty @Size(max = 20) List<@Valid OrderLineRequest> lines,
        @Size(max = 500) @Pattern(regexp = "https?://.+", message = "must be an http or https URL") String notifyUrl) {

    /** A stable text form of the request, used to spot a reused Idempotency-Key with a different body. */
    String fingerprint() {
        String lineText = lines.stream()
                .map(line -> line.productId() + "x" + line.quantity())
                .collect(Collectors.joining(","));
        return lineText + "|" + (notifyUrl == null ? "" : notifyUrl);
    }

}
