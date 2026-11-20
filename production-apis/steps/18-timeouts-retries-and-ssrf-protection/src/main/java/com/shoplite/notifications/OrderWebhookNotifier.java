package com.shoplite.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.FilteredHostException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrderWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrderWebhookNotifier.class);

    private final RestClient webhookRestClient;

    public OrderWebhookNotifier(RestClient webhookRestClient) {
        this.webhookRestClient = webhookRestClient;
    }

    /**
     * Tells the customer's system that an order is paid. A failure here is logged, never
     * thrown back, because the order itself already succeeded.
     */
    public void send(String url, OrderWebhookPayload payload) {
        try {
            webhookRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Webhook {} for order {} delivered to {}", payload.event(), payload.orderId(), url);
        } catch (FilteredHostException ex) {
            // The SSRF filter refused the address. This exception is not a RestClientException,
            // so it needs its own catch.
            log.warn("Webhook for order {} blocked: {} is not a public address", payload.orderId(), ex.getHost());
        } catch (RestClientException ex) {
            log.warn("Webhook for order {} to {} failed: {}", payload.orderId(), url, ex.getMessage());
        }
    }

}
