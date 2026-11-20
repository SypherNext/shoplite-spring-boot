package com.shoplite.payments;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers PaymentProviderClient as a bean in the "payments" group.
 * Spring Boot reads spring.http.serviceclient.payments.base-url for that group.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "payments", types = PaymentProviderClient.class)
public class PaymentsConfig {
}
