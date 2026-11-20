package com.shoplite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** The shopping assistant. Like the other services, it scans com.shoplite to find libs/web-common. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AssistantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantServiceApplication.class, args);
    }

}
