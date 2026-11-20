package com.shoplite.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.TestJwts;

/**
 * @RecordApplicationEvents keeps every event published during a test, so the test can
 * check that checkout announced what happened, with the right data.
 */
@RecordApplicationEvents
class OrderEventsTests extends IntegrationTestSupport {

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void resetPaymentProvider() {
        paymentProvider.resetAll();
    }

    @Test
    void aPaidOrderPublishesOrderPaid() {
        charge("APPROVED");

        placeOrder(4, 2); // two notebooks at 9.80

        assertThat(events.stream(OrderPaid.class)).singleElement().satisfies(paid -> {
            assertThat(paid.total()).isEqualByComparingTo(new BigDecimal("19.60"));
            assertThat(paid.lines()).containsExactly(new OrderEvent.Line(4L, 2));
        });
        assertThat(events.stream(OrderPaymentFailed.class)).isEmpty();
    }

    @Test
    void aDeclinedOrderPublishesOrderPaymentFailed() {
        charge("DECLINED");

        placeOrder(1, 1);

        assertThat(events.stream(OrderPaymentFailed.class)).singleElement()
                .satisfies(failed -> assertThat(failed.lines()).containsExactly(new OrderEvent.Line(1L, 1)));
        assertThat(events.stream(OrderPaid.class)).isEmpty();
    }

    private void placeOrder(long productId, int quantity) {
        assertThat(mvc.post().uri("/api/orders")
                .with(TestJwts.customer("alice"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"lines": [{"productId": %d, "quantity": %d}]}
                        """.formatted(productId, quantity)))
                .hasStatus2xxSuccessful();
    }

    private static void charge(String status) {
        paymentProvider.stubFor(post(urlPathEqualTo("/v1/charges")).willReturn(okJson("""
                {"paymentId": "pay_events_1", "status": "%s"}
                """.formatted(status))));
    }

}
