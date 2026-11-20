package com.shoplite.orders;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes Idempotency-Keys older than the retention period, so the table does not grow forever.
 * Note: with several app instances, every instance runs this job. That is harmless here
 * (deleting already-deleted rows does nothing); jobs that must run once need a lock such as ShedLock.
 */
@Component
public class IdempotencyKeyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyCleanupJob.class);

    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;

    public IdempotencyKeyCleanupJob(IdempotencyService idempotencyService, IdempotencyProperties properties) {
        this.idempotencyService = idempotencyService;
        this.properties = properties;
    }

    @Scheduled(cron = "${shoplite.idempotency.cleanup-cron}")
    public void deleteExpiredKeys() {
        Instant cutoff = Instant.now().minus(properties.retention());
        int deleted = idempotencyService.deleteCreatedBefore(cutoff);
        log.info("Idempotency cleanup removed {} keys created before {}", deleted, cutoff);
    }

}
