package com.shoplite.outbox;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * shoplite.outbox.*: how often the relay looks for new rows, how many it sends per round,
 * how long it waits for Kafka to confirm one, and how long sent rows are kept.
 */
@ConfigurationProperties("shoplite.outbox")
public record OutboxProperties(
        @DefaultValue("500ms") Duration pollInterval,
        @DefaultValue("100") int batchSize,
        @DefaultValue("5s") Duration sendTimeout,
        @DefaultValue("7d") Duration retention) {
}
