package com.shoplite.orders;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Counts finished checkouts: /actuator/metrics/shoplite.orders.completed?tag=outcome:paid
 *
 * @TransactionalEventListener runs after the transaction commits (AFTER_COMMIT is the default).
 * If the commit fails, the order did not really change, and nothing is counted.
 */
@Component
class OrderMetrics {

    private final MeterRegistry meterRegistry;

    OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener
    void on(OrderPaid event) {
        count("paid");
    }

    @TransactionalEventListener
    void on(OrderPaymentFailed event) {
        count("payment_failed");
    }

    private void count(String outcome) {
        meterRegistry.counter("shoplite.orders.completed", "outcome", outcome).increment();
    }

}
