package com.shoplite.payments;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;

/**
 * An HTTP service client: Spring writes the implementation of this interface for you.
 * The base URL and timeouts come from spring.http.serviceclient.payments.* (see PaymentsConfig).
 */
public interface PaymentProviderClient {

    // The provider uses the Idempotency-Key to recognise a retried charge and not charge twice.
    @PostExchange("/v1/charges")
    ChargeResponse charge(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ChargeRequest request);

}
