package com.shoplite.outbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends outbox rows to Kafka, oldest first, and stamps each one as published.
 *
 * At least once: if the app dies after Kafka accepted a message but before the stamp is
 * committed, the row is sent again after the restart. Consumers must cope with duplicates.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final OutboxProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxRepository repository, OutboxProperties properties,
            ProducerFactory<String, String> producerFactory) {
        this.repository = repository;
        this.properties = properties;
        // The payload is already JSON text, so this template sends it as plain text.
        this.kafkaTemplate = new KafkaTemplate<>(producerFactory, Map.of(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
    }

    @Scheduled(fixedDelayString = "${shoplite.outbox.poll-interval:500ms}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = repository.lockUnpublished(properties.batchSize());
        for (OutboxEvent event : batch) {
            try {
                // get(): wait until Kafka confirms the message is stored, then stamp the row.
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
                event.markPublished();
                log.info("Outbox event {} {} for key {} published to {}", event.getId(), event.getEventType(),
                        event.getMessageKey(), event.getTopic());
            } catch (Exception ex) {
                // Stop here, so the remaining rows keep their order. The next round tries again.
                log.warn("Outbox relay could not publish event {} ({} waiting): {}", event.getId(),
                        repository.countByPublishedAtIsNull(), ex.getMessage());
                return;
            }
        }
    }

}
