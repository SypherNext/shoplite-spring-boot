package com.shoplite.notifications;

/** A webhook waiting to be sent: where to, and what to post. Travels through RabbitMQ as JSON. */
public record WebhookDelivery(String url, OrderWebhookPayload payload) {
}
