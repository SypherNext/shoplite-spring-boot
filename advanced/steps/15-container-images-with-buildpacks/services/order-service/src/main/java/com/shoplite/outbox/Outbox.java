package com.shoplite.outbox;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Puts a message in the outbox. Any module can use it; it knows nothing about orders.
 */
@Component
public class Outbox {

    private final OutboxRepository repository;
    private final JsonMapper jsonMapper;

    public Outbox(OutboxRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    /**
     * MANDATORY: there must already be a transaction (the one that changes the order).
     * Called without one, this fails at once instead of quietly saving the message on its own.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void add(UUID id, String topic, String key, String eventType, Object message) {
        repository.save(new OutboxEvent(id, topic, key, eventType, jsonMapper.writeValueAsString(message)));
    }

}
