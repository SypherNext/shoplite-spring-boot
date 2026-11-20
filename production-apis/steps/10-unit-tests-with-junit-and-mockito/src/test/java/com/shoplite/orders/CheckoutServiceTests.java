package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.shoplite.common.UnprocessableException;
import com.shoplite.customers.Customer;

/**
 * Unit tests with Mockito: CheckoutService runs for real, everything it talks to is a mock.
 * No Spring context, so these run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTests {

    @Mock
    OrderService orderService;
    @Mock
    IdempotencyService idempotencyService;

    CheckoutService checkoutService;
    Customer alice = new Customer("sub-alice", "alice@shoplite.test", "Alice");

    private static final OrderRequest REQUEST = new OrderRequest(List.of(new OrderLineRequest(4L, 2)));

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(orderService, idempotencyService);
        // Entities get their id from the database, so the test sets it by hand.
        ReflectionTestUtils.setField(alice, "id", 1L);
    }

    @Test
    void newKeyCreatesTheOrder() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.empty());
        given(orderService.createOrder(eq(alice), eq(REQUEST), eq("key-1"), anyString())).willReturn(10L);
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.order().id()).isEqualTo(10L);
        assertThat(result.replayed()).isFalse();
    }

    @Test
    void replayReturnsTheEarlierOrderAndCreatesNothing() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.of(10L));
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.replayed()).isTrue();
        then(orderService).should(never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void theKeyIsLookedUpWithAHashOfTheBody() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.of(10L));
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L));

        checkoutService.placeOrder(alice, REQUEST, "key-1");

        // One line: product 4, quantity 2.
        then(idempotencyService).should().findOrderId(1L, "key-1", IdempotencyService.hash("4x2"));
    }

    @Test
    void aReusedKeyWithADifferentBodyStopsBeforeAnyOrderIsCreated() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString()))
                .willThrow(new UnprocessableException("Idempotency-Key key-1 was already used with a different request body"));

        assertThatThrownBy(() -> checkoutService.placeOrder(alice, REQUEST, "key-1"))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("key-1");
        then(orderService).shouldHaveNoInteractions();
    }

    private static OrderDetails order(Long id) {
        return new OrderDetails(id, OrderStatus.PAID, new BigDecimal("19.60"), "EUR", Instant.now(), "alice@shoplite.test",
                null, 0, List.of());
    }

}
