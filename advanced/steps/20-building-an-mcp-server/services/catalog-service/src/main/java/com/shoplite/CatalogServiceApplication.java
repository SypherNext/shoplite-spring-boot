package com.shoplite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The catalog service. Component scanning starts at com.shoplite, so it also finds the shared
 * classes in libs/web-common (com.shoplite.common, com.shoplite.shop, com.shoplite.security).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }

}
