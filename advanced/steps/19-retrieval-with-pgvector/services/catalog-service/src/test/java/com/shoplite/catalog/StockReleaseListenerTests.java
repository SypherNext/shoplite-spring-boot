package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.KafkaTestTopics;

/** Order events arriving from Kafka, as the order service's outbox sends them. */
class StockReleaseListenerTests extends IntegrationTestSupport {

    private static final String TOPIC = "shoplite.order-events";

    @Autowired
    KafkaAdmin kafkaAdmin;

    KafkaTestTopics topics;

    @BeforeEach
    void setUp() {
        topics = new KafkaTestTopics(kafkaAdmin);
    }

    @Test
    void theSameMessageTwiceReleasesTheStockOnce() {
        int stockBefore = stockOf(3);
        String message = """
                {"eventId":"%s","type":"PAYMENT_FAILED","orderId":999,"lines":[{"productId":3,"quantity":1}]}
                """.formatted(UUID.randomUUID());

        topics.send(TOPIC, "999", message);
        topics.send(TOPIC, "999", message);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(stockOf(3)).isEqualTo(stockBefore + 1));
        await().pollDelay(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(stockOf(3)).isEqualTo(stockBefore + 1));
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

}
