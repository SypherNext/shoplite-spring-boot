package com.shoplite.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.FilteredHostException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
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
     * @Async: the caller does not wait. The order response goes back to the customer
     * while this runs on another (virtual) thread. A failure here is logged, never thrown
     * back, because the order itself already succeeded.
     */
    @Async
    public void send(String url, OrderWebhookPayload payload) {
        log.debug("Sending webhook for order {} on {} (virtual: {})", payload.orderId(),
                Thread.currentThread().getName(), Thread.currentThread().isVirtual());
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
