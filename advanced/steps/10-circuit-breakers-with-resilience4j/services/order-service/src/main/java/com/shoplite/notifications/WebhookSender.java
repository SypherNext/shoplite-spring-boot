package com.shoplite.notifications;

import org.springframework.boot.http.client.FilteredHostException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * POSTs one webhook. It throws when the delivery fails, and says whether trying again could help.
 */
@Component
public class WebhookSender {

    private final RestClient webhookRestClient;

    public WebhookSender(RestClient webhookRestClient) {
        this.webhookRestClient = webhookRestClient;
    }

    /**
     * @throws WebhookRejectedException for failures a retry cannot fix: a blocked address
     *         (SSRF filter) or a 4xx answer. Anything else (timeouts, refused connections,
     *         5xx answers) is thrown as it is, and is worth another try.
     */
    public void send(WebhookDelivery delivery) {
        try {
            webhookRestClient.post()
                    .uri(delivery.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(delivery.payload())
                    .retrieve()
                    .toBodilessEntity();
        } catch (FilteredHostException ex) {
            throw new WebhookRejectedException(ex.getHost() + " is not a public address", ex);
        } catch (HttpClientErrorException ex) {
            throw new WebhookRejectedException("the receiver answered " + ex.getStatusCode(), ex);
        }
    }

}
