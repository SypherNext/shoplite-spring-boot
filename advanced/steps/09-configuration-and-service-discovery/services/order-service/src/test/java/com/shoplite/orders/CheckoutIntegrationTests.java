package com.shoplite.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.TestJwts;

/**
 * The checkout through the real order service: security, transactions, PostgreSQL, the
 * payment client with retries, and the catalog client with token relay. The catalog and the
 * payment provider are WireMock servers.
 */
class CheckoutIntegrationTests extends IntegrationTestSupport {

    private static final String RESERVATIONS = "/internal/stock/reservations";
    private static final String RELEASE = "/internal/stock/reservations/[0-9a-f-]+/release";
    private static final String CHARGES = "/v1/charges";

    @BeforeEach
    void reset() {
        paymentProvider.resetAll();
        catalog.resetAll();
    }

    @Test
    void reservesStockWithTheCustomersTokenThenPays() {
        catalogReservesNotebooks(2);
        paymentAnswers("APPROVED");

        MvcTestResult result = placeOrder("alice", newKey(), orderJson(4, 2));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(json(result, "$.status").toString()).isEqualTo("PAID");
        assertThat(json(result, "$.total").toString()).isEqualTo("19.6");
        assertThat(json(result, "$.lines[0].sku").toString()).isEqualTo("NOTE-A5-DOT");
        // Token relay: the catalog saw the customer's own bearer token.
        catalog.verify(1, postRequestedFor(urlPathEqualTo(RESERVATIONS))
                .withHeader("Authorization", matching("Bearer .+")));
    }

    @Test
    void sameKeyTwiceReservesAndChargesOnce() {
        catalogReservesNotebooks(1);
        paymentAnswers("APPROVED");
        String key = newKey();

        MvcTestResult first = placeOrder("alice", key, orderJson(4, 1));
        MvcTestResult second = placeOrder("alice", key, orderJson(4, 1));

        assertThat(second).hasStatus(HttpStatus.OK).hasHeader("Idempotent-Replayed", "true");
        assertThat((Integer) json(second, "$.id")).isEqualTo(json(first, "$.id"));
        catalog.verify(1, postRequestedFor(urlPathEqualTo(RESERVATIONS)));
        paymentProvider.verify(1, postRequestedFor(urlPathEqualTo(CHARGES)));
    }

    @Test
    void catalogConflictIsPassedOnAndNothingIsCharged() {
        catalog.stubFor(post(urlPathEqualTo(RESERVATIONS)).willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("""
                        {"title": "Conflict", "status": 409, "detail": "Product DESK-STAND-OAK is not for sale"}""")));

        assertThat(placeOrder("alice", newKey(), orderJson(6, 1)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.detail", detail -> assertThat(detail).isEqualTo("Product DESK-STAND-OAK is not for sale"));
        paymentProvider.verify(0, postRequestedFor(urlPathEqualTo(CHARGES)));
    }

    @Test
    void catalogDownGives503AndNothingIsSaved() {
        catalog.stubFor(post(urlPathEqualTo(RESERVATIONS)).willReturn(aResponse().withStatus(500)));

        assertThat(placeOrder("alice", newKey(), orderJson(4, 1)))
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .hasHeader("Retry-After", "5");
        paymentProvider.verify(0, postRequestedFor(urlPathEqualTo(CHARGES)));
    }

    @Test
    void anOrderThatCannotBeSavedGivesTheStockBack() {
        // The catalog reserves a product whose SKU is too long for the order_lines column (40 characters),
        // so saving the order fails AFTER the stock was taken. The checkout must release it again.
        catalogReserves(4, 1, "X".repeat(41), "Too long", "9.80");

        assertThat(placeOrder("alice", newKey(), orderJson(4, 1)).getResponse().getStatus()).isGreaterThanOrEqualTo(400);
        catalog.verify(1, postRequestedFor(urlPathMatching(RELEASE)));
        paymentProvider.verify(0, postRequestedFor(urlPathEqualTo(CHARGES)));
    }

    @Test
    void providerDownGives503AndTheSameKeyFinishesTheOrderLater() {
        catalogReservesNotebooks(1);
        paymentProvider.stubFor(post(urlPathEqualTo(CHARGES)).willReturn(aResponse().withStatus(503)));
        String key = newKey();

        assertThat(placeOrder("alice", key, orderJson(4, 1))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);

        paymentProvider.resetAll();
        paymentAnswers("APPROVED");
        MvcTestResult retry = placeOrder("alice", key, orderJson(4, 1));
        assertThat(retry).hasStatus(HttpStatus.OK).hasHeader("Idempotent-Replayed", "true");
        assertThat(json(retry, "$.status").toString()).isEqualTo("PAID");
        catalog.verify(1, postRequestedFor(urlPathEqualTo(RESERVATIONS)));
        paymentProvider.verify(1, postRequestedFor(urlPathEqualTo(CHARGES))
                .withHeader("Idempotency-Key", matching("order-\\d+")));
    }

    @Test
    void aRetryAfterACatalogTimeoutReusesTheSameReservation() {
        // The catalog answers after 3 s, but the client gives up after 2 s (read-timeout): 503.
        // The catalog may well have reserved the stock anyway.
        catalog.stubFor(post(urlPathEqualTo(RESERVATIONS)).willReturn(okJson("""
                [{"id": 4, "sku": "NOTE-A5-DOT", "name": "Dotted Notebook A5", "price": 9.80}]""")
                .withFixedDelay(3000)));
        paymentAnswers("APPROVED");
        String key = newKey();

        assertThat(placeOrder("alice", key, orderJson(4, 1))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
        String firstReservation = reservationIdOfLastCall();

        // The client tries again with the same Idempotency-Key, and the catalog is fast now.
        catalog.resetAll();
        catalogReservesNotebooks(1);
        assertThat(placeOrder("alice", key, orderJson(4, 1))).hasStatus(HttpStatus.CREATED);

        // Same reservation id both times: the catalog answers the retry from its table.
        assertThat(reservationIdOfLastCall()).isEqualTo(firstReservation);
    }

    private static String reservationIdOfLastCall() {
        return JsonPath.read(catalog.findAll(postRequestedFor(urlPathEqualTo(RESERVATIONS))).getLast()
                .getBodyAsString(), "$.reservationId");
    }
    @Test
    void declinedPaymentFailsTheOrder() {        catalogReservesNotebooks(5);
        paymentAnswers("DECLINED");

        MvcTestResult result = placeOrder("alice", newKey(), orderJson(4, 5));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(json(result, "$.status").toString()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void customersCannotSeeEachOthersOrders() {
        catalogReservesNotebooks(1);
        paymentAnswers("APPROVED");
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(4, 1)), "$.id");

        assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestJwts.customer("bob")))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void version2ResponseHasAMoneyObject() {
        catalogReservesNotebooks(3);
        paymentAnswers("APPROVED");
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(4, 3)), "$.id");

        assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestJwts.customer("alice"))
                .header("API-Version", "2.0"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total.currency", currency -> assertThat(currency).isEqualTo("EUR"))
                .hasPathSatisfying("$.itemCount", count -> assertThat(count).isEqualTo(3));
    }

    @Test
    void ordersNeedACustomerAndAdminPagesNeedAnAdmin() {
        assertThat(mvc.post().uri("/api/orders").header("Idempotency-Key", newKey())
                .contentType(MediaType.APPLICATION_JSON).content(orderJson(4, 1)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/admin/orders").with(TestJwts.customer("alice")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/admin/orders?status=PAID").with(TestJwts.admin()))
                .hasStatusOk();
    }

}
