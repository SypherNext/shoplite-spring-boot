package com.shoplite;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.math.BigDecimal;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Base class for full order-service tests: real PostgreSQL, Kafka and RabbitMQ (Testcontainers),
 * and two WireMock servers standing in for the payment provider and the catalog service.
 * Every subclass has the same configuration, so Spring builds the context once.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    // http2PlainDisabled: the JDK HTTP client's h2c upgrade fails against WireMock (see course 2).
    protected static final WireMockServer paymentProvider =
            new WireMockServer(options().dynamicPort().http2PlainDisabled(true));
    protected static final WireMockServer catalog =
            new WireMockServer(options().dynamicPort().http2PlainDisabled(true));

    static {
        paymentProvider.start();
        catalog.start();
    }

    @DynamicPropertySource
    static void fakeServices(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.payments.base-url", paymentProvider::baseUrl);
        registry.add("spring.http.serviceclient.catalog.base-url", catalog::baseUrl);
    }

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    Resilience4JCircuitBreakerFactory circuitBreakers;

    /** Every test starts with a closed catalog circuit, whatever the test before it did. */
    @BeforeEach
    void closeTheCatalogCircuit() {
        circuitBreakers.getCircuitBreakerRegistry().circuitBreaker("catalog").reset();
    }

    /** The fake catalog reserves this product for any order of that quantity. */
    protected static void catalogReserves(long productId, int quantity, String sku, String name, String price) {
        catalog.stubFor(post(urlPathEqualTo("/internal/stock/reservations"))
                // true, true: ignore array order and extra fields (the reservationId changes per order).
                .withRequestBody(equalToJson("""
                        {"lines": [{"productId": %d, "quantity": %d}]}""".formatted(productId, quantity), true, true))
                .willReturn(okJson("""
                        [{"id": %d, "sku": "%s", "name": "%s", "price": %s}]"""
                        .formatted(productId, sku, name, new BigDecimal(price)))));
    }

    /** The two notebooks most tests order: NOTE-A5-DOT at 9.80. */
    protected static void catalogReservesNotebooks(int quantity) {
        catalogReserves(4, quantity, "NOTE-A5-DOT", "Dotted Notebook A5", "9.80");
    }

    protected static void paymentAnswers(String status) {
        paymentProvider.stubFor(post(urlPathEqualTo("/v1/charges")).willReturn(okJson("""
                {"paymentId": "pay_test_123", "status": "%s"}
                """.formatted(status))));
    }

    protected MvcTestResult placeOrder(String customer, String key, String body) {
        return mvc.post().uri("/api/orders")
                .with(TestJwts.customer(customer))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    protected static String orderJson(long productId, int quantity) {
        return """
                {"lines": [{"productId": %d, "quantity": %d}]}
                """.formatted(productId, quantity);
    }

    protected static String newKey() {
        return UUID.randomUUID().toString();
    }

    /** Reads one value out of a JSON response body, for example json(result, "$.id"). */
    protected static <T> T json(MvcTestResult result, String path) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(), path);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
