package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.shoplite.IntegrationTestSupport;

/** @RecordApplicationEvents keeps every event published during a test. */
@RecordApplicationEvents
class OrderEventsTests extends IntegrationTestSupport {

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void reset() {
        paymentProvider.resetAll();
        catalog.resetAll();
        catalogReservesNotebooks(2);
    }

    @Test
    void aPaidOrderPublishesOrderPaid() {
        paymentAnswers("APPROVED");

        placeOrder("alice", newKey(), orderJson(4, 2));

        assertThat(events.stream(OrderPaid.class)).singleElement().satisfies(paid -> {
            assertThat(paid.total()).isEqualByComparingTo(new BigDecimal("19.60"));
            assertThat(paid.lines()).containsExactly(new OrderEvent.Line(4L, 2));
        });
    }

    @Test
    void aDeclinedOrderPublishesOrderPaymentFailed() {
        paymentAnswers("DECLINED");

        placeOrder("alice", newKey(), orderJson(4, 2));

        assertThat(events.stream(OrderPaymentFailed.class)).singleElement()
                .satisfies(failed -> assertThat(failed.lines()).containsExactly(new OrderEvent.Line(4L, 2)));
        assertThat(events.stream(OrderPaid.class)).isEmpty();
    }

}
