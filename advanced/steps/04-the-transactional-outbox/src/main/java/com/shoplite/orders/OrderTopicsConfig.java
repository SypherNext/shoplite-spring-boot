package com.shoplite.orders;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Spring Boot's KafkaAdmin creates these topics at startup if they do not exist yet. */
@Configuration(proxyBeanMethods = false)
class OrderTopicsConfig {

    // 3 partitions: up to 3 consumers in one group can share the work.
    // 1 replica: there is only one broker in development. Production clusters use 3.
    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OrderEventMessage.TOPIC).partitions(3).replicas(1).build();
    }

}
