package com.shoplite.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

import com.shoplite.IntegrationTestSupport;

/**
 * Webhook deliveries through a real RabbitMQ broker (Testcontainers). The HTTP call itself is
 * replaced by a mock, so each test decides how the receiver behaves.
 */
class WebhookDeliveryTests extends IntegrationTestSupport {

    private static final WebhookDelivery DELIVERY = new WebhookDelivery("https://example.com/hooks/orders",
            new OrderWebhookPayload("order.paid", 42L, "PAID", new BigDecimal("19.60"), "EUR"));

    @Autowired
    RabbitTemplate rabbitTemplate;

    @MockitoBean
    WebhookSender sender;

    @BeforeEach
    void emptyTheDeadLetterQueue() {
        while (rabbitTemplate.receive(WebhookQueues.FAILED) != null) {
            // drain messages left by an earlier test
        }
    }

    @Test
    void aDeliveredWebhookLeavesNothingBehind() {
        queue();

        assertThat(rabbitTemplate.receive(WebhookQueues.FAILED, 3000)).isNull();
        then(sender).should().send(DELIVERY);
    }

    @Test
    void aRejectedWebhookIsDeadLetteredWithoutRetries() {
        willThrow(new WebhookRejectedException("localhost is not a public address", null)).given(sender).send(any());
        Instant start = Instant.now();

        queue();

        assertThat(takeFailed()).isEqualTo(DELIVERY);
        assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(3));
        then(sender).should(times(1)).send(any());
    }

    @Test
    void aTemporaryFailureIsRetriedTwiceThenDeadLettered() {
        willThrow(new ResourceAccessException("I/O error: Connect timed out")).given(sender).send(any());
        Instant start = Instant.now();

        queue();

        assertThat(takeFailed()).isEqualTo(DELIVERY);
        // 3 attempts, with 1 s and then 2 s between them.
        assertThat(Duration.between(start, Instant.now())).isGreaterThanOrEqualTo(Duration.ofSeconds(3));
        then(sender).should(times(3)).send(any());
    }

    private void queue() {
        rabbitTemplate.convertAndSend(WebhookQueues.EXCHANGE, WebhookQueues.ROUTING_KEY, DELIVERY);
    }

    private WebhookDelivery takeFailed() {
        WebhookDelivery failed = rabbitTemplate.receiveAndConvert(WebhookQueues.FAILED, 20_000,
                new ParameterizedTypeReference<WebhookDelivery>() { });
        assertThat(failed).as("a message in " + WebhookQueues.FAILED).isNotNull();
        return failed;
    }

}
