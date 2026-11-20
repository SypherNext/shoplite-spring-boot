package com.shoplite.notifications;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Duration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.FilteredHostException;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.web.client.RestClient;

/**
 * Shows the SSRF protection on the webhook client. A WireMock server on localhost plays
 * the part of an internal service an attacker wants to reach through a webhook URL.
 */
class WebhookSsrfTests {

    private final WireMockServer internalService = new WireMockServer(options().dynamicPort().http2PlainDisabled(true));
    private final RestClient webhookClient = new WebhookClientConfig().webhookRestClient(RestClient.builder(),
            new WebhookProperties(Duration.ofSeconds(1), Duration.ofSeconds(1)));

    @BeforeEach
    void start() {
        internalService.start();
        internalService.stubFor(post(anyUrl()).willReturn(ok()));
    }

    @AfterEach
    void stop() {
        internalService.stop();
    }

    @Test
    void externalAddressesFilterAllowsOnlyPublicAddresses() throws Exception {
        InetAddressFilter filter = InetAddressFilter.externalAddresses();
        assertThat(filter.matches(InetAddress.getByName("127.0.0.1"))).isFalse();       // loopback
        assertThat(filter.matches(InetAddress.getByName("10.0.0.5"))).isFalse();        // private network
        assertThat(filter.matches(InetAddress.getByName("169.254.169.254"))).isFalse(); // cloud metadata
        assertThat(filter.matches(InetAddress.getByName("8.8.8.8"))).isTrue();          // public internet
    }

    @Test
    void webhookClientRefusesToCallLocalhost() {
        String url = "http://127.0.0.1:" + internalService.port() + "/actuator/shutdown";

        assertThatThrownBy(() -> webhookClient.post().uri(url).retrieve().toBodilessEntity())
                .isInstanceOf(FilteredHostException.class)
                .hasMessage("Filtered host '127.0.0.1'");
        internalService.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void notifierLogsTheFailureInsteadOfThrowing() {
        OrderWebhookNotifier notifier = new OrderWebhookNotifier(webhookClient);

        // send() catches the SSRF refusal and logs it, so nothing is thrown here.
        notifier.send("http://localhost:" + internalService.port() + "/hook",
                new OrderWebhookPayload("order.paid", 1L, "PAID", new BigDecimal("9.80"), "EUR"));

        internalService.verify(0, anyRequestedFor(anyUrl()));
    }

}
