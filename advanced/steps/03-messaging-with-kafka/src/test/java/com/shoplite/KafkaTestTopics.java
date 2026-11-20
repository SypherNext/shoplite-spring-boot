package com.shoplite;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Plain Kafka clients for tests: send raw text to a topic, or read everything a topic holds.
 * They use Strings, so a test sees exactly the bytes on the wire.
 */
public final class KafkaTestTopics {

    private final String bootstrapServers;

    public KafkaTestTopics(KafkaAdmin admin) {
        // Spring Boot fills KafkaAdmin with the Testcontainers broker address (a String or a List).
        Object servers = admin.getConfigurationProperties().get("bootstrap.servers");
        this.bootstrapServers = servers instanceof Collection<?> list
                ? String.join(",", list.stream().map(Object::toString).toList())
                : servers.toString();
    }

    public void send(String topic, String key, String value) {
        Map<String, Object> props = Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (var producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(topic, key, value));
        }
    }

    /** Reads the topic from the beginning until no new record arrives for a moment. */
    public List<ConsumerRecord<String, String>> readAll(String topic, Duration wait) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        try (var consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + wait.toMillis();
            while (System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(records::add);
            }
        }
        return records;
    }

}
