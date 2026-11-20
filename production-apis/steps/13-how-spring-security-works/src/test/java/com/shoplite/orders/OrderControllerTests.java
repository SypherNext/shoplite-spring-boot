package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.shoplite.customers.Customer;
import com.shoplite.customers.CustomerService;
import com.shoplite.security.SecurityConfig;

/**
 * A web slice test: only the MVC layer (this controller, JSON, validation, versioning, security
 * filters) is started. Services are Mockito mocks, so no database is needed.
 */
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTests {

    private static final String ALICE = "alice@shoplite.test";

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    CheckoutService checkoutService;
    @MockitoBean
    OrderService orderService;
    @MockitoBean
    CustomerService customerService;

    @BeforeEach
    void setUp() {
        given(customerService.currentCustomer(ALICE)).willReturn(new Customer(ALICE, ALICE, "alice"));
        given(orderService.findForCustomer(eq(7L), any())).willReturn(paidOrder());
    }

    @Test
    void noVersionHeaderMeansVersion1() {
        assertThat(mvc.get().uri("/api/orders/7").with(user("alice")).header("Customer-Email", ALICE))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total", total -> assertThat(total).isEqualTo(29.4))
                .hasPathSatisfying("$.createdAt", createdAt -> assertThat(createdAt).isNotNull());
    }

    @Test
    void version2HasAMoneyObjectAndAnItemCount() {
        assertThat(mvc.get().uri("/api/orders/7").with(user("alice")).header("Customer-Email", ALICE).header("API-Version", "2.0"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total.amount", amount -> assertThat(amount).isEqualTo(29.4))
                .hasPathSatisfying("$.total.currency", currency -> assertThat(currency).isEqualTo("EUR"))
                .hasPathSatisfying("$.itemCount", count -> assertThat(count).isEqualTo(3));
    }

    @Test
    void unknownVersionIsABadRequest() {
        assertThat(mvc.get().uri("/api/orders/7").with(user("alice")).header("Customer-Email", ALICE).header("API-Version", "3.0"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void placingAnOrderNeedsAnIdempotencyKey() {
        assertThat(mvc.post().uri("/api/orders").with(user("alice")).header("Customer-Email", ALICE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"lines": [{"productId": 4, "quantity": 1}]}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").asString().contains("Idempotency-Key");
    }

    @Test
    void aReplayAnswers200WithAHeader() {
        given(checkoutService.placeOrder(any(), any(), eq("key-1"))).willReturn(new CheckoutResult(paidOrder(), true));

        assertThat(mvc.post().uri("/api/orders").with(user("alice")).header("Customer-Email", ALICE)
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"lines": [{"productId": 4, "quantity": 3}]}
                        """))
                .hasStatusOk()
                .hasHeader("Idempotent-Replayed", "true");
    }

    @Test
    void ordersNeedASignedInUser() {
        assertThat(mvc.get().uri("/api/orders/7").header("Customer-Email", ALICE))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theCustomerHeaderMustBeAnEmail() {
        assertThat(mvc.get().uri("/api/orders/7").with(user("alice")).header("Customer-Email", "not-an-email"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidOrdersListEveryProblemByName() {
        assertThat(mvc.post().uri("/api/orders").with(user("alice")).header("Customer-Email", ALICE)
                .header("Idempotency-Key", " ")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"lines": []}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.title", title -> assertThat(title).isEqualTo("Validation failed"))
                .hasPathSatisfying("$.errors.lines", message -> assertThat(message).isEqualTo("must not be empty"))
                .hasPathSatisfying("$.errors['Idempotency-Key']",
                        message -> assertThat(message).isEqualTo("must not be blank"));
    }

    private static OrderDetails paidOrder() {
        OrderLineResponse line = new OrderLineResponse(4L, "NOTE-A5-DOT", "Dotted Notebook A5",
                new BigDecimal("9.80"), 3, new BigDecimal("29.40"));
        return new OrderDetails(7L, OrderStatus.PAID, new BigDecimal("29.40"), "EUR", Instant.now(),
                ALICE, null, 1, List.of(line));
    }

}
