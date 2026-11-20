package com.shoplite.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * The oldest unpublished rows, locked for this transaction. SKIP LOCKED: rows another app
     * instance is already sending are skipped instead of waited for, so two instances never
     * send the same batch at the same time.
     */
    @Query(value = """
            select * from outbox_events
            where published_at is null
            order by created_at
            limit :batchSize
            for update skip locked""", nativeQuery = true)
    List<OutboxEvent> lockUnpublished(int batchSize);

    long countByPublishedAtIsNull();

    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt < :cutoff")
    int deletePublishedBefore(Instant cutoff);

}
