package com.shoplite.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.shoplite.IntegrationTestSupport;

/** The Event Publication Registry records the OrderPaid event and completes it once the listener ran. */
class EventPublicationTests extends IntegrationTestSupport {

    @Autowired
    JdbcClient jdbc;

    @Test
    void theWebhookListenerCompletesItsPublication() {
        catalog.resetAll();
        paymentProvider.resetAll();
        catalogReservesNotebooks(1);
        paymentAnswers("APPROVED");
        Integer orderId = json(placeOrder("alice", newKey(), """
                {"lines": [{"productId": 4, "quantity": 1}], "notifyUrl": "https://example.com/hooks"}
                """), "$.id");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.sql("""
                        select status from event_publication
                        where listener_id like '%OrderPaidWebhookListener%'
                          and serialized_event like :order""")
                        .param("order", "%\"orderId\":" + orderId + ",%")
                        .query(String.class).list())
                        .containsExactly("COMPLETED"));
    }

}
