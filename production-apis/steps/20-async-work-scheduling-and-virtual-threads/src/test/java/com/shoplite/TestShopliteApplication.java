package com.shoplite;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app against a Testcontainers database instead of compose.yaml.
 * Start it with: ./gradlew bootTestRun
 */
public class TestShopliteApplication {

    public static void main(String[] args) {
        SpringApplication.from(ShopliteApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
