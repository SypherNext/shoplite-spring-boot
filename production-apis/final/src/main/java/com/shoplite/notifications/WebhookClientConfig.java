package com.shoplite.notifications;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * The RestClient for customer webhooks. Customers choose these URLs, so an attacker could
 * point one at http://localhost:8080/actuator, a database, or a cloud metadata address
 * (Server-Side Request Forgery, SSRF). The InetAddressFilter checks the resolved IP address
 * and only lets the request through to public internet addresses.
 *
 * The filter is applied to THIS client only. The payment client talks to a trusted,
 * configured address (localhost in development), so it must not be filtered.
 */
@Configuration(proxyBeanMethods = false)
public class WebhookClientConfig {

    @Bean
    RestClient webhookRestClient(RestClient.Builder builder, WebhookProperties properties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withInetAddressFilter(InetAddressFilter.externalAddresses())
                .withTimeouts(properties.connectTimeout(), properties.readTimeout())
                // A redirect could bounce the call to an internal address, so do not follow any.
                .withRedirects(HttpRedirects.DONT_FOLLOW);
        // clone(): the injected builder is shared, and changes to it would leak into other clients.
        return builder.clone()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

}
