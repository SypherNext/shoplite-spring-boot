package com.shoplite.payments;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentProviderClient paymentProviderClient;

    public PaymentService(PaymentProviderClient paymentProviderClient) {
        this.paymentProviderClient = paymentProviderClient;
    }

    /**
     * Charges an order. Retries only failures that may go away by themselves
     * (timeouts, refused connections, 5xx): 1 call + up to 2 retries, waiting 200 ms then 400 ms.
     * A DECLINED answer is a normal result and is never retried.
     * At most 10 charges run at the same time, so a slow provider cannot tie up every thread.
     */
    @Retryable(includes = PaymentUnavailableException.class, maxRetries = 2, delay = 200, multiplier = 2)
    @ConcurrencyLimit(10)
    public ChargeResponse charge(String orderReference, BigDecimal amount, String currency) {
        try {
            ChargeResponse response = paymentProviderClient.charge(orderReference,
                    new ChargeRequest(orderReference, amount, currency));
            log.info("Payment for {}: {} ({})", orderReference, response.status(), response.paymentId());
            return response;
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.warn("Payment provider failed for {}: {}", orderReference, ex.getMessage());
            throw new PaymentUnavailableException(ex);
        }
    }

}
