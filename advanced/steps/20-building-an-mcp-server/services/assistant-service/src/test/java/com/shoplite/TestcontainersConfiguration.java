package com.shoplite;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL with pgvector in a container, and embeddings that need no model (see TopicEmbeddingModel). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"));
    }

    @Bean
    @Primary
    EmbeddingModel topicEmbeddings() {
        return new TopicEmbeddingModel();
    }

}
