package com.shoplite.notifications;

import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot builds the listener retry policy from spring.rabbitmq.listener.simple.retry.*.
 * By default it retries every exception, even a rejection that can never succeed.
 * This customizer adds one rule: a WebhookRejectedException anywhere in the cause chain is
 * not retried, so the message goes straight to the dead letter queue.
 */
@Configuration(proxyBeanMethods = false)
class WebhookRetryConfig {

    @Bean
    RabbitListenerRetrySettingsCustomizer skipRetriesForRejectedWebhooks() {
        return settings -> settings.setExceptionPredicate(WebhookRetryConfig::worthRetrying);
    }

    static boolean worthRetrying(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof WebhookRejectedException) {
                return false;
            }
        }
        return true;
    }

}
