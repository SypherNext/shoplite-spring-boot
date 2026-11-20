package com.shoplite.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.TestJwts;

/**
 * The whole checkout through the real stack: security, validation, transactions,
 * PostgreSQL, the HTTP service client, and a WireMock payment provider.
 */
class CheckoutIntegrationTests extends IntegrationTestSupport {

    private static final String CHARGES = "/v1/charges";
    private static final long NOTEBOOK_ID = 4; // NOTE-A5-DOT, 9.80, stock 75 in the seed data

    @BeforeEach
    void resetPaymentProvider() {
        paymentProvider.resetAll();
    }

    @Test
    void placesAndPaysAnOrder() {
        approveEveryCharge();
        int stockBefore = stockOf(NOTEBOOK_ID);

        MvcTestResult result = placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 2));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(json(result, "$.status").toString()).isEqualTo("PAID");
        assertThat(json(result, "$.total").toString()).isEqualTo("19.6");
        assertThat(stockOf(NOTEBOOK_ID)).isEqualTo(stockBefore - 2);
    }

    @Test
    void sameKeyTwiceReturnsTheFirstOrderAndChargesOnce() {
        approveEveryCharge();
        String key = newKey();

        MvcTestResult first = placeOrder("alice", key, orderJson(NOTEBOOK_ID, 1));
        MvcTestResult second = placeOrder("alice", key, orderJson(NOTEBOOK_ID, 1));

        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(second).hasStatus(HttpStatus.OK).hasHeader("Idempotent-Replayed", "true");
        Integer orderId = json(first, "$.id");
        assertThat((Integer) json(second, "$.id")).isEqualTo(orderId);
        paymentProvider.verify(1, postRequestedFor(urlPathEqualTo(CHARGES))
                .withHeader("Idempotency-Key", equalTo("order-" + orderId)));
    }

    @Test
    void sameKeyWithADifferentBodyIsRefused() {
        approveEveryCharge();
        String key = newKey();
        placeOrder("alice", key, orderJson(NOTEBOOK_ID, 1));

        assertThat(placeOrder("alice", key, orderJson(NOTEBOOK_ID, 3)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void declinedPaymentFailsTheOrderAndReturnsTheStock() {
        paymentProvider.stubFor(post(urlPathEqualTo(CHARGES))
                .withRequestBody(matchingJsonPath("$.reference"))
                .willReturn(okJson("""
                        {"paymentId": "pay_declined", "status": "DECLINED"}
                        """)));
        int stockBefore = stockOf(NOTEBOOK_ID);

        MvcTestResult result = placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 5));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(json(result, "$.status").toString()).isEqualTo("PAYMENT_FAILED");
        assertThat(stockOf(NOTEBOOK_ID)).isEqualTo(stockBefore);
    }

    @Test
    void productNotForSaleIsAConflictAndNothingIsCharged() {
        assertThat(placeOrder("alice", newKey(), orderJson(6, 1))) // the monitor stand is inactive
                .hasStatus(HttpStatus.CONFLICT);
        paymentProvider.verify(0, postRequestedFor(urlPathEqualTo(CHARGES)));
    }

    @Test
    void version2ResponseHasAMoneyObject() {
        approveEveryCharge();
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 3)), "$.id");

        assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestJwts.customer("alice"))
                .header("API-Version", "2.0"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total.currency", currency -> assertThat(currency).isEqualTo("EUR"))
                .hasPathSatisfying("$.itemCount", count -> assertThat(count).isEqualTo(3));
    }

    @Test
    void customersCannotSeeEachOthersOrders() {
        approveEveryCharge();
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 1)), "$.id");

        assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestJwts.customer("bob")))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminStatusChangeWithAStaleVersionIsRefused() {
        approveEveryCharge();
        MvcTestResult placed = placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 1));
        Integer orderId = json(placed, "$.id");
        Integer version = json(placed, "$.version");

        assertThat(changeStatus(orderId, "SHIPPED", version - 1)).hasStatus(HttpStatus.CONFLICT);
        assertThat(changeStatus(orderId, "SHIPPED", version))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.version").isEqualTo(version + 1);
    }

    @Test
    void ordersNeedACustomerAndAdminPagesNeedAnAdmin() {
        assertThat(mvc.post().uri("/api/orders").header("Idempotency-Key", newKey())
                .contentType(MediaType.APPLICATION_JSON).content(orderJson(NOTEBOOK_ID, 1)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/admin/orders").with(TestJwts.customer("alice")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/admin/orders?status=PAID").with(TestJwts.admin()))
                .hasStatusOk();
    }

    private MvcTestResult placeOrder(String customer, String key, String body) {
        return mvc.post().uri("/api/orders")
                .with(TestJwts.customer(customer))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult changeStatus(Integer orderId, String status, Integer version) {
        return mvc.patch().uri("/api/admin/orders/{id}/status", orderId)
                .with(TestJwts.admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "%s", "version": %d}
                        """.formatted(status, version))
                .exchange();
    }

    private int stockOf(long productId) {
        return json(mvc.get().uri("/api/products/{id}", productId).exchange(), "$.stock");
    }

    private static void approveEveryCharge() {
        paymentProvider.stubFor(post(urlPathEqualTo(CHARGES)).willReturn(okJson("""
                {"paymentId": "pay_test_123", "status": "APPROVED"}
                """)));
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    private static String orderJson(long productId, int quantity) {
        return """
                {"lines": [{"productId": %d, "quantity": %d}]}
                """.formatted(productId, quantity);
    }

}
