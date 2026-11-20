package com.shoplite.notifications;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Timeouts for calls to customer webhook URLs (shoplite.webhooks.*). */
@ConfigurationProperties("shoplite.webhooks")
public record WebhookProperties(
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {
}
