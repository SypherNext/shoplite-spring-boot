package com.shoplite.outbox;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.IllegalTransactionStateException;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.TestJwts;

class OutboxTests extends IntegrationTestSupport {

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    Outbox outbox;

    @BeforeEach
    void approveCharges() {
        paymentProvider.resetAll();
        paymentProvider.stubFor(post(urlPathEqualTo("/v1/charges")).willReturn(okJson("""
                {"paymentId": "pay_outbox_1", "status": "APPROVED"}
                """)));
    }

    @Test
    void aPaidOrderLeavesAnOutboxRowThatTheRelayPublishes() {
        Integer orderId = json(mvc.post().uri("/api/orders")
                .with(TestJwts.customer("alice"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"lines": [{"productId": 4, "quantity": 1}]}
                        """)
                .exchange(), "$.id");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxRepository.findAll())
                        .filteredOn(event -> event.getMessageKey().equals(orderId.toString()))
                        .singleElement()
                        .satisfies(event -> {
                            assertThat(event.getEventType()).isEqualTo("PAID");
                            assertThat(event.getPayload()).contains("\"orderId\": " + orderId);
                            assertThat(event.getPublishedAt()).isNotNull();
                        }));
    }

    @Test
    void addingToTheOutboxNeedsATransaction() {
        assertThatThrownBy(() -> outbox.add(UUID.randomUUID(), "some-topic", "key", "TEST", "message"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

}
