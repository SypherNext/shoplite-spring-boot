package com.shoplite.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.shoplite.orders.OrderPaid;

/**
 * Sends the "order.paid" webhook when an order is paid.
 *
 * AFTER_COMMIT (the default phase): the webhook only goes out once the PAID status is
 * really in the database. A rolled-back payment never tells the customer "paid".
 * OrderWebhookNotifier.send is @Async, so the checkout request does not wait for it.
 */
@Component
public class OrderPaidWebhookListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidWebhookListener.class);

    private final OrderWebhookNotifier webhookNotifier;

    public OrderPaidWebhookListener(OrderWebhookNotifier webhookNotifier) {
        this.webhookNotifier = webhookNotifier;
    }

    @TransactionalEventListener
    public void on(OrderPaid event) {
        log.debug("Order {} paid, committed; webhook url: {}", event.orderId(), event.notifyUrl());
        if (event.notifyUrl() == null) {
            return;
        }
        webhookNotifier.send(event.notifyUrl(), new OrderWebhookPayload("order.paid", event.orderId(), "PAID",
                event.total(), event.currency()));
    }

}
