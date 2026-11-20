package com.shoplite.notifications;

import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shoplite.orders.OrderEvent;
import com.shoplite.orders.OrderPaid;

/** A listener is a plain method: call it with an event and check what it did. */
@ExtendWith(MockitoExtension.class)
class OrderPaidWebhookListenerTests {

    @Mock
    OrderWebhookNotifier webhookNotifier;

    @Test
    void sendsTheWebhookToTheOrdersUrl() {
        var listener = new OrderPaidWebhookListener(webhookNotifier);

        listener.on(paid("https://example.com/hooks/orders"));

        then(webhookNotifier).should().send("https://example.com/hooks/orders",
                new OrderWebhookPayload("order.paid", 7L, "PAID", new BigDecimal("34.80"), "EUR"));
    }

    @Test
    void ordersWithoutAUrlSendNothing() {
        var listener = new OrderPaidWebhookListener(webhookNotifier);

        listener.on(paid(null));

        then(webhookNotifier).shouldHaveNoInteractions();
    }

    private static OrderPaid paid(String notifyUrl) {
        return new OrderPaid(7L, 1L, new BigDecimal("34.80"), "EUR", notifyUrl, List.of(new OrderEvent.Line(1L, 2)));
    }

}
