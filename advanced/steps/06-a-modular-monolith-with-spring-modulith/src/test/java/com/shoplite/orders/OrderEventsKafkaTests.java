package com.shoplite.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.KafkaTestTopics;
import com.shoplite.TestJwts;

/** Order events through a real Kafka broker (Testcontainers). */
class OrderEventsKafkaTests extends IntegrationTestSupport {

    private static final String TOPIC = "shoplite.order-events";

    @Autowired
    KafkaAdmin kafkaAdmin;

    KafkaTestTopics topics;

    @BeforeEach
    void setUp() {
        topics = new KafkaTestTopics(kafkaAdmin);
        paymentProvider.resetAll();
    }

    @Test
    void aPaidOrderIsPublishedWithTheOrderIdAsKey() {
        charge("APPROVED");
        Integer orderId = json(placeOrder(4, 1), "$.id");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(topics.readAll(TOPIC, Duration.ofSeconds(2)))
                        .anySatisfy(record -> {
                            assertThat(record.key()).isEqualTo(orderId.toString());
                            // The outbox stores jsonb, and PostgreSQL writes jsonb back with a space after each colon.
                            assertThat(record.value()).contains("\"type\": \"PAID\"", "\"productId\": 4");
                        }));
    }

    @Test
    void theSameMessageTwiceReleasesTheStockOnce() {
        // Kafka delivers at least once: a consumer can see one message again (after a crash,
        // a rebalance, or a relay that sent a row twice). processed_events remembers the event id.
        int stockBefore = stockOf(3);
        String message = """
                {"eventId":"%s","type":"PAYMENT_FAILED","orderId":999,"lines":[{"productId":3,"quantity":1}]}
                """.formatted(UUID.randomUUID());

        topics.send(TOPIC, "999", message);
        topics.send(TOPIC, "999", message);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(stockOf(3)).isEqualTo(stockBefore + 1));
        // Give the second copy time to arrive, then check it changed nothing.
        await().pollDelay(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(stockOf(3)).isEqualTo(stockBefore + 1));
    }

    @Test
    void aMessageThatIsNotJsonGoesToTheDeadLetterTopic() {
        topics.send(TOPIC, "poison-1", "this is not JSON");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(topics.readAll(TOPIC + "-dlt", Duration.ofSeconds(2)))
                        .anySatisfy(record -> {
                            assertThat(record.key()).isEqualTo("poison-1");
                            assertThat(new String(record.headers().lastHeader("kafka_dlt-exception-fqcn").value()))
                                    .contains("DeserializationException");
                        }));
    }

    private MvcTestResult placeOrder(long productId, int quantity) {
        return mvc.post().uri("/api/orders")
                .with(TestJwts.customer("alice"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"lines": [{"productId": %d, "quantity": %d}]}
                        """.formatted(productId, quantity))
                .exchange();
    }

    private int stockOf(long productId) {
        return json(mvc.get().uri("/api/products/{id}", productId).exchange(), "$.stock");
    }

    private static void charge(String status) {
        paymentProvider.stubFor(post(urlPathEqualTo("/v1/charges")).willReturn(okJson("""
                {"paymentId": "pay_kafka_1", "status": "%s"}
                """.formatted(status))));
    }

}
