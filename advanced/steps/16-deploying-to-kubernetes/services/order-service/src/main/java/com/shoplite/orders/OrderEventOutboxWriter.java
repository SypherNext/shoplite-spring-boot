package com.shoplite.orders;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.shoplite.outbox.Outbox;

/**
 * Writes every OrderEvent into the outbox.
 *
 * A plain @EventListener runs synchronously, inside the transaction that published the event.
 * So the outbox row and the order change are committed together, or rolled back together.
 * Compare lesson 3, where the Kafka send ran after the commit and could be lost.
 */
@Component
class OrderEventOutboxWriter {

    private final Outbox outbox;

    OrderEventOutboxWriter(Outbox outbox) {
        this.outbox = outbox;
    }

    @EventListener
    void write(OrderEvent event) {
        OrderEventMessage message = OrderEventMessage.from(event);
        outbox.add(message.eventId(), OrderEventMessage.TOPIC, event.orderId().toString(), message.type(), message);
    }

}
