package com.shoplite.catalog;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remembers which events the catalog has handled, so a message delivered twice is applied once.
 * This is what makes the consumer idempotent.
 */
@Component
class ProcessedEvents {

    private final JdbcClient jdbc;

    ProcessedEvents(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Records the event and returns true the first time; returns false if it was already there.
     * One INSERT does both the check and the write, so two copies of the same message arriving at
     * the same moment cannot both win. MANDATORY: it must share the transaction that changes the
     * stock, so "handled" is only remembered if the stock change is saved too.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    boolean firstTime(UUID eventId) {
        int inserted = jdbc.sql("""
                insert into processed_events (event_id, processed_at) values (:id, now())
                on conflict (event_id) do nothing""")
                .param("id", eventId)
                .update();
        return inserted == 1;
    }

}
