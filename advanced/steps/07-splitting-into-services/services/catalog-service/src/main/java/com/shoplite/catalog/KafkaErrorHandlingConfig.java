package com.shoplite.catalog;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * What happens when a listener throws. Spring Boot attaches a CommonErrorHandler bean to the
 * listener containers by itself.
 *
 * A failing message is tried 3 times in total (1 + 2 retries, 1 second apart). If it still fails,
 * it is copied to "<topic>-dlt" (the dead letter topic) and the consumer moves on, so one bad
 * message cannot block every message behind it. A message that cannot even be read as JSON
 * goes to the dead letter topic at once: retrying would not change it.
 */
@Configuration(proxyBeanMethods = false)
class KafkaErrorHandlingConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 2L));
    }

    // The catalog declares the topic it reads, too. Whichever service starts first creates it with
    // 3 partitions; without this, the broker auto-created it with 1 partition when the catalog
    // subscribed first, and the catalog then saw only partition 0 for several minutes.
    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OrderStockMessage.TOPIC).partitions(3).replicas(1).build();
    }

    // The recoverer writes a failed record to the SAME partition number in the dead letter topic,
    // so that topic needs as many partitions as the original (3).
    @Bean
    NewTopic orderEventsDeadLetterTopic() {
        return TopicBuilder.name(OrderStockMessage.TOPIC + "-dlt").partitions(3).replicas(1).build();
    }

}
