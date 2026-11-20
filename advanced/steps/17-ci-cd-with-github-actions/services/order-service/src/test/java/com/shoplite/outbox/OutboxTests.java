package com.shoplite.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

import com.shoplite.IntegrationTestSupport;

class OutboxTests extends IntegrationTestSupport {

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    Outbox outbox;

    @Test
    void aPaidOrderLeavesAnOutboxRowThatTheRelayPublishes() {
        catalog.resetAll();
        paymentProvider.resetAll();
        catalogReservesNotebooks(1);
        paymentAnswers("APPROVED");
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(4, 1)), "$.id");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxRepository.findAll())
                        .filteredOn(event -> event.getMessageKey().equals(orderId.toString()))
                        .singleElement()
                        .satisfies(event -> {
                            assertThat(event.getEventType()).isEqualTo("PAID");
                            assertThat(event.getPublishedAt()).isNotNull();
                        }));
    }

    @Test
    void addingToTheOutboxNeedsATransaction() {
        assertThatThrownBy(() -> outbox.add(UUID.randomUUID(), "some-topic", "key", "TEST", "message"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

}
