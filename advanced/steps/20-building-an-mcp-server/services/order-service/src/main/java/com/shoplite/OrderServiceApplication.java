package com.shoplite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The order service. Inside, it is still a Spring Modulith modular monolith:
 * orders, customers, payments, notifications and outbox are its modules.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
