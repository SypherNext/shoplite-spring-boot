package com.shoplite.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.HttpStatus;

import com.shoplite.IntegrationTestSupport;

/** The circuit breaker around the catalog calls, with WireMock playing a broken catalog. */
class CatalogCircuitBreakerTests extends IntegrationTestSupport {

    private static final String RESERVATIONS = "/internal/stock/reservations";

    @Autowired
    Resilience4JCircuitBreakerFactory factory;

    @BeforeEach
    void reset() {
        catalog.resetAll();
        paymentProvider.resetAll();
    }

    @Test
    void fiveFailuresOpenTheCircuitAndTheSixthCallNeverLeaves() {
        catalog.stubFor(post(urlPathEqualTo(RESERVATIONS)).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 5; i++) {
            assertThat(placeOrder("alice", newKey(), orderJson(4, 1))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
        }
        assertThat(state()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThat(placeOrder("alice", newKey(), orderJson(4, 1))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
        catalog.verify(5, postRequestedFor(urlPathEqualTo(RESERVATIONS)));
    }

    @Test
    void outOfStockAnswersDoNotCount() {
        catalog.stubFor(post(urlPathEqualTo(RESERVATIONS)).willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("""
                        {"title": "Conflict", "status": 409, "detail": "Only 0 of NOTE-A5-DOT left in stock"}""")));

        for (int i = 0; i < 6; i++) {
            assertThat(placeOrder("alice", newKey(), orderJson(4, 1))).hasStatus(HttpStatus.CONFLICT);
        }
        assertThat(state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private CircuitBreaker.State state() {
        return factory.getCircuitBreakerRegistry().circuitBreaker("catalog").getState();
    }

}