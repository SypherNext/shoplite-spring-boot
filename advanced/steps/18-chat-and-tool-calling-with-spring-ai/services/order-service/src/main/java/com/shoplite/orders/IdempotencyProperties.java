package com.shoplite.orders;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** How long Idempotency-Keys are remembered, and when old ones are cleaned up (shoplite.idempotency.*). */
@ConfigurationProperties("shoplite.idempotency")
public record IdempotencyProperties(
        @DefaultValue("24h") Duration retention,
        @DefaultValue("0 0 * * * *") String cleanupCron) {
}
