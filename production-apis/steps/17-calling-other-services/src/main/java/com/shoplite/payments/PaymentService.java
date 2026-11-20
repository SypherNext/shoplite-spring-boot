package com.shoplite.payments;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentProviderClient paymentProviderClient;

    public PaymentService(PaymentProviderClient paymentProviderClient) {
        this.paymentProviderClient = paymentProviderClient;
    }

    /** Charges an order. A DECLINED answer is a normal result, not an error. */
    public ChargeResponse charge(String orderReference, BigDecimal amount, String currency) {
        ChargeResponse response = paymentProviderClient.charge(orderReference,
                new ChargeRequest(orderReference, amount, currency));
        log.info("Payment for {}: {} ({})", orderReference, response.status(), response.paymentId());
        return response;
    }

}
