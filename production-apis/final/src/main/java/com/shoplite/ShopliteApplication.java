package com.shoplite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // finds every @ConfigurationProperties record under com.shoplite
public class ShopliteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopliteApplication.class, args);
    }

}
