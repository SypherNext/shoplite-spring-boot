package com.shoplite.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends every OrderEvent to Kafka once the order change is committed.
 *
 * The weak spot (lesson 4 fixes it): the database commit and the Kafka send are two separate
 * steps. If Kafka is down right after the commit, the order is saved but the message is lost.
 */
@Component
class OrderEventKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaPublisher.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    OrderEventKafkaPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener
    void publish(OrderEvent event) {
        OrderEventMessage message = OrderEventMessage.from(event);
        // The order id is the key: every message for one order lands on the same partition,
        // so consumers see that order's events in the order they happened.
        String key = event.orderId().toString();
        try {
            kafkaTemplate.send(OrderEventMessage.TOPIC, key, message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Order event {} for order {} was NOT delivered: {}", message.type(), key, ex.getMessage());
                } else {
                    log.info("Order event {} for order {} sent to partition {} offset {}", message.type(), key,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (KafkaException ex) {
            // Thrown at once when the broker cannot even be reached within max.block.ms.
            log.error("Order event {} for order {} was NOT delivered: {}", message.type(), key, ex.getMessage());
        }
    }

}
