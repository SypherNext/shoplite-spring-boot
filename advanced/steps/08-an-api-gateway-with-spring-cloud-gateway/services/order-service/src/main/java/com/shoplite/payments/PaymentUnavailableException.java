package com.shoplite.payments;

import com.shoplite.common.ServiceUnavailableException;

/** The provider did not answer, or answered with a 5xx error. Safe to retry. */
public class PaymentUnavailableException extends ServiceUnavailableException {

    public PaymentUnavailableException(Throwable cause) {
        super("The payment provider is not responding. Retry with the same Idempotency-Key", cause);
    }

}
