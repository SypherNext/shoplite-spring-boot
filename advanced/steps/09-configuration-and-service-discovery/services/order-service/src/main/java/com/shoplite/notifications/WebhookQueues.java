package com.shoplite.notifications;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The RabbitMQ layout for webhooks. Spring AMQP declares all of it on the broker at startup.
 *
 *   producer --"delivery"--> exchange shoplite.webhooks --> queue shoplite.webhooks.deliveries
 *   a rejected message ------> exchange shoplite.webhooks.dlx --> queue shoplite.webhooks.failed
 *
 * The deliveries queue names a dead letter exchange (x-dead-letter-exchange): a message the
 * worker rejects, after its retries, is moved there instead of being dropped.
 */
@Configuration(proxyBeanMethods = false)
class WebhookQueues {

    static final String EXCHANGE = "shoplite.webhooks";
    static final String ROUTING_KEY = "delivery";
    static final String DELIVERIES = "shoplite.webhooks.deliveries";
    static final String DEAD_LETTER_EXCHANGE = "shoplite.webhooks.dlx";
    static final String FAILED = "shoplite.webhooks.failed";

    @Bean
    Declarables webhookTopology() {
        DirectExchange exchange = new DirectExchange(EXCHANGE);
        Queue deliveries = QueueBuilder.durable(DELIVERIES).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
        FanoutExchange deadLetters = new FanoutExchange(DEAD_LETTER_EXCHANGE);
        Queue failed = QueueBuilder.durable(FAILED).build();
        Binding toDeliveries = BindingBuilder.bind(deliveries).to(exchange).with(ROUTING_KEY);
        Binding toFailed = BindingBuilder.bind(failed).to(deadLetters);
        return new Declarables(exchange, deliveries, deadLetters, failed, toDeliveries, toFailed);
    }

    /** Messages as JSON (Jackson 3). Spring Boot uses this bean for RabbitTemplate and the listeners. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
