package com.shoplite.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Logs every state change of the catalog circuit breaker, so you can see it open and close. */
@Configuration(proxyBeanMethods = false)
class CatalogCircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(CatalogCircuitBreakerConfig.class);

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> logCatalogStateChanges() {
        return factory -> factory.addCircuitBreakerCustomizer(
                breaker -> breaker.getEventPublisher().onStateTransition(
                        event -> log.warn("Circuit breaker '{}': {}", event.getCircuitBreakerName(),
                                event.getStateTransition())),
                "catalog");
    }

}