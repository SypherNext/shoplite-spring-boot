package com.shoplite.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Takes webhooks off the queue and sends them.
 *
 * - Success: the method returns, Spring acknowledges the message, RabbitMQ deletes it.
 * - Temporary failure (timeout, refused connection, 5xx): the exception goes back to Spring AMQP,
 *   which retries (spring.rabbitmq.listener.simple.retry.*). After the last retry the message is
 *   rejected, and RabbitMQ moves it to the dead letter queue shoplite.webhooks.failed.
 * - Permanent failure (WebhookRejectedException): WebhookRetryConfig says "do not retry", so it
 *   is rejected and dead-lettered at once.
 */
@Component
class WebhookDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryWorker.class);

    private final WebhookSender sender;

    WebhookDeliveryWorker(WebhookSender sender) {
        this.sender = sender;
    }

    @RabbitListener(queues = WebhookQueues.DELIVERIES)
    void deliver(WebhookDelivery delivery) {
        Long orderId = delivery.payload().orderId();
        try {
            sender.send(delivery);
            log.info("Webhook for order {} delivered to {}", orderId, delivery.url());
        } catch (WebhookRejectedException ex) {
            log.warn("Webhook for order {} rejected, not retrying: {}", orderId, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Webhook for order {} to {} failed, may retry: {}", orderId, delivery.url(), ex.getMessage());
            throw ex;
        }
    }

}
