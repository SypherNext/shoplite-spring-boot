package com.shoplite.payments;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers PaymentProviderClient as a bean in the "payments" group.
 * Spring Boot reads spring.http.serviceclient.payments.base-url, connect-timeout and read-timeout
 * for that group. @EnableResilientMethods switches on @Retryable and @ConcurrencyLimit.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "payments", types = PaymentProviderClient.class)
@EnableResilientMethods
public class PaymentsConfig {
}
