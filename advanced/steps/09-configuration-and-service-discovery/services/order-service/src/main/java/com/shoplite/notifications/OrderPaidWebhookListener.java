package com.shoplite.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.shoplite.orders.OrderPaid;

/**
 * When an order is paid (and committed), queue its webhook in RabbitMQ.
 * The HTTP call happens later, in WebhookDeliveryWorker, with retries and a dead letter queue.
 *
 * @ApplicationModuleListener = @TransactionalEventListener (after commit) + @Async + its own new
 * transaction. With the Event Publication Registry, Spring Modulith stores the event before the
 * commit and marks it completed only when this method returns. If RabbitMQ is down, the method
 * throws, the publication stays incomplete, and it is sent again later (see application.properties).
 */
@Component
public class OrderPaidWebhookListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidWebhookListener.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderPaidWebhookListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @ApplicationModuleListener
    public void on(OrderPaid event) {
        if (event.notifyUrl() == null) {
            return;
        }
        var payload = new OrderWebhookPayload("order.paid", event.orderId(), "PAID", event.total(), event.currency());
        rabbitTemplate.convertAndSend(WebhookQueues.EXCHANGE, WebhookQueues.ROUTING_KEY,
                new WebhookDelivery(event.notifyUrl(), payload));
        log.debug("Queued webhook for order {}", event.orderId());
    }

}
