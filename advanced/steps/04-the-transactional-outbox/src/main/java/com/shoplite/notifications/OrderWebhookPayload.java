package com.shoplite.notifications;

import java.math.BigDecimal;

/** The JSON body POSTed to a customer's webhook URL when an order is paid. */
public record OrderWebhookPayload(String event, Long orderId, String status, BigDecimal total, String currency) {
}
