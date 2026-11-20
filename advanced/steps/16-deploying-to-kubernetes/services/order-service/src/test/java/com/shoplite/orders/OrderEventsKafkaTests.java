package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.KafkaTestTopics;

/** A declined order reaches Kafka through the outbox, keyed by the order id. */
class OrderEventsKafkaTests extends IntegrationTestSupport {

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Test
    void aDeclinedOrderIsPublishedForTheCatalog() {
        catalog.resetAll();
        paymentProvider.resetAll();
        catalogReservesNotebooks(1);
        paymentAnswers("DECLINED");
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(4, 1)), "$.id");

        KafkaTestTopics topics = new KafkaTestTopics(kafkaAdmin);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(topics.readAll("shoplite.order-events", Duration.ofSeconds(2)))
                        .anySatisfy(record -> {
                            assertThat(record.key()).isEqualTo(orderId.toString());
                            // jsonb from the outbox: PostgreSQL writes a space after each colon.
                            assertThat(record.value()).contains("\"type\": \"PAYMENT_FAILED\"", "\"productId\": 4");
                        }));
    }

}
