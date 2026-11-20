package com.shoplite.outbox;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Deletes published outbox rows after the retention period (default 7 days), once an hour. */
@Component
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxRepository repository;
    private final OutboxProperties properties;

    public OutboxCleanupJob(OutboxRepository repository, OutboxProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void deletePublished() {
        Instant cutoff = Instant.now().minus(properties.retention());
        int deleted = repository.deletePublishedBefore(cutoff);
        log.info("Outbox cleanup removed {} events published before {}", deleted, cutoff);
    }

}
