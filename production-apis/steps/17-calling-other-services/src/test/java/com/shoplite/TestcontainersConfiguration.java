package com.shoplite;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway PostgreSQL container for tests.
 * {@code @ServiceConnection} hands its URL, user and password to Spring Boot,
 * so no datasource properties are needed.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // Same image as compose.yaml, so tests run against the database you develop on.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18.7-alpine"));
    }

}
