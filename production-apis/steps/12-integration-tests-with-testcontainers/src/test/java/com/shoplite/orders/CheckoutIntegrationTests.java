package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.shoplite.IntegrationTestSupport;

/**
 * The whole checkout through the real stack: validation, transactions, the idempotency table
 * and PostgreSQL. Tests share one database, so each one reads the stock before it changes it.
 */
class CheckoutIntegrationTests extends IntegrationTestSupport {

    private static final long NOTEBOOK_ID = 4; // NOTE-A5-DOT, 9.80, stock 75 in the seed data
    private static final long PENS_ID = 5;     // PEN-GEL-5PK, stock 2 in the seed data

    @Test
    void placesAnOrderAndTakesStock() {
        int stockBefore = stockOf(NOTEBOOK_ID);

        MvcTestResult result = placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 2));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(json(result, "$.total").toString()).isEqualTo("19.6");
        assertThat(stockOf(NOTEBOOK_ID)).isEqualTo(stockBefore - 2);
    }

    @Test
    void sameKeyTwiceReturnsTheFirstOrderAndTakesStockOnce() {
        String key = newKey();
        int stockBefore = stockOf(NOTEBOOK_ID);

        MvcTestResult first = placeOrder("alice", key, orderJson(NOTEBOOK_ID, 1));
        MvcTestResult second = placeOrder("alice", key, orderJson(NOTEBOOK_ID, 1));

        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(second).hasStatus(HttpStatus.OK).hasHeader("Idempotent-Replayed", "true");
        assertThat((Integer) json(second, "$.id")).isEqualTo(json(first, "$.id"));
        assertThat(stockOf(NOTEBOOK_ID)).isEqualTo(stockBefore - 1);
    }

    @Test
    void sameKeyWithADifferentBodyIsRefused() {
        String key = newKey();
        placeOrder("alice", key, orderJson(NOTEBOOK_ID, 1));

        assertThat(placeOrder("alice", key, orderJson(NOTEBOOK_ID, 3)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void anOrderThatCannotBeServedLeavesNoTrace() {
        int notebooksBefore = stockOf(NOTEBOOK_ID);
        String twoLines = """
                {"lines": [{"productId": %d, "quantity": 1}, {"productId": %d, "quantity": 3}]}
                """.formatted(NOTEBOOK_ID, PENS_ID);

        assertThat(placeOrder("alice", newKey(), twoLines))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Only 2 of PEN-GEL-5PK left in stock");
        assertThat(stockOf(NOTEBOOK_ID)).isEqualTo(notebooksBefore);
    }

    @Test
    void productNotForSaleIsAConflict() {
        assertThat(placeOrder("alice", newKey(), orderJson(6, 1))) // the monitor stand is inactive
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void version2ResponseHasAMoneyObject() {
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 3)), "$.id");

        assertThat(mvc.get().uri("/api/orders/{id}", orderId).header("Customer-Email", email("alice"))
                .header("API-Version", "2.0"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total.currency", currency -> assertThat(currency).isEqualTo("EUR"))
                .hasPathSatisfying("$.itemCount", count -> assertThat(count).isEqualTo(3));
    }

    @Test
    void customersCannotSeeEachOthersOrders() {
        Integer orderId = json(placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 1)), "$.id");

        assertThat(mvc.get().uri("/api/orders/{id}", orderId).header("Customer-Email", email("bob")))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminStatusChangeWithAStaleVersionIsRefused() {
        MvcTestResult placed = placeOrder("alice", newKey(), orderJson(NOTEBOOK_ID, 1));
        Integer orderId = json(placed, "$.id");
        Integer version = json(placed, "$.version");

        assertThat(changeStatus(orderId, "SHIPPED", version - 1)).hasStatus(HttpStatus.CONFLICT);
        assertThat(changeStatus(orderId, "SHIPPED", version))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.version").isEqualTo(version + 1);
    }

    private MvcTestResult placeOrder(String customer, String key, String body) {
        return mvc.post().uri("/api/orders")
                .header("Customer-Email", email(customer))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult changeStatus(Integer orderId, String status, Integer version) {
        return mvc.patch().uri("/api/admin/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "%s", "version": %d}
                        """.formatted(status, version))
                .exchange();
    }

    private int stockOf(long productId) {
        return json(mvc.get().uri("/api/products/{id}", productId).exchange(), "$.stock");
    }

    private static String email(String name) {
        return name + "@shoplite.test";
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
