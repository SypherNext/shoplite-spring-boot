package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.TestJwts;

/** The internal stock API that the order service calls during checkout. */
class StockReservationTests extends IntegrationTestSupport {

    @Test
    void reservesEveryLineAndAnswersWithTheProducts() {
        int mugsBefore = stockOf(1);

        MvcTestResult result = reserve(UUID.randomUUID(), """
                [{"productId": 1, "quantity": 2}, {"productId": 4, "quantity": 1}]""");

        assertThat(result).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].sku", sku -> assertThat(sku).isEqualTo("MUG-ENAMEL-01"))
                .hasPathSatisfying("$[1].price", price -> assertThat(price).asNumber().isEqualTo(9.8));
        assertThat(stockOf(1)).isEqualTo(mugsBefore - 2);
    }

    @Test
    void theSameReservationIdTwiceReservesOnce() {
        int mugsBefore = stockOf(1);
        UUID id = UUID.randomUUID();

        assertThat(reserve(id, "[{\"productId\": 1, \"quantity\": 1}]")).hasStatusOk();
        assertThat(reserve(id, "[{\"productId\": 1, \"quantity\": 1}]")).hasStatusOk();

        assertThat(stockOf(1)).isEqualTo(mugsBefore - 1);
    }

    @Test
    void oneBadLineReservesNothing() {
        int mugsBefore = stockOf(1);

        // Product 6, the monitor stand, is not for sale: the whole reservation is refused.
        assertThat(reserve(UUID.randomUUID(), """
                [{"productId": 1, "quantity": 1}, {"productId": 6, "quantity": 1}]"""))
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(stockOf(1)).isEqualTo(mugsBefore);
    }

    @Test
    void releasingTwiceGivesTheStockBackOnce() {
        int bottlesBefore = stockOf(2);
        UUID id = UUID.randomUUID();
        reserve(id, "[{\"productId\": 2, \"quantity\": 1}]");

        assertThat(release(id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(release(id)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(stockOf(2)).isEqualTo(bottlesBefore);
    }

    @Test
    void needsACustomerToken() {
        assertThat(mvc.post().uri("/internal/stock/reservations").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reservationId": "%s", "lines": [{"productId": 1, "quantity": 1}]}"""
                        .formatted(UUID.randomUUID())))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult reserve(UUID id, String lines) {
        return mvc.post().uri("/internal/stock/reservations").with(TestJwts.customer("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reservationId": "%s", "lines": %s}""".formatted(id, lines))
                .exchange();
    }

    private MvcTestResult release(UUID id) {
        return mvc.post().uri("/internal/stock/reservations/{id}/release", id).with(TestJwts.customer("alice"))
                .exchange();
    }

}
