package com.shoplite.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One message waiting in the outbox table until the relay sends it to Kafka. */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    // The message exactly as it will go to Kafka, stored as PostgreSQL jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    OutboxEvent(UUID id, String topic, String messageKey, String eventType, String payload) {
        this.id = id;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    void markPublished() {
        this.publishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

}
