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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.shoplite.customers.Customer;
import com.shoplite.notifications.OrderWebhookNotifier;
import com.shoplite.notifications.OrderWebhookPayload;
import com.shoplite.payments.ChargeResponse;
import com.shoplite.payments.PaymentService;
import com.shoplite.payments.PaymentUnavailableException;

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
    @Mock
    PaymentService paymentService;
    @Mock
    OrderWebhookNotifier webhookNotifier;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    CheckoutService checkoutService;
    Customer alice = new Customer("sub-alice", "alice@shoplite.test", "Alice");

    private static final OrderRequest REQUEST = new OrderRequest(List.of(new OrderLineRequest(4L, 2)),
            "https://example.com/hooks/orders");

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(orderService, idempotencyService, paymentService, webhookNotifier,
                meterRegistry);
        // Entities get their id from the database, so the test sets it by hand.
        ReflectionTestUtils.setField(alice, "id", 1L);
    }

    @Test
    void newOrderIsChargedMarkedPaidAndAnnounced() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.empty());
        given(orderService.createPendingOrder(eq(alice), eq(REQUEST), eq("key-1"), anyString())).willReturn(10L);
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L, OrderStatus.PENDING_PAYMENT));
        given(paymentService.charge("order-10", new BigDecimal("19.60"), "EUR"))
                .willReturn(new ChargeResponse("pay_1", "APPROVED"));
        given(orderService.markPaid(10L, "pay_1")).willReturn(order(10L, OrderStatus.PAID));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.order().status()).isEqualTo(OrderStatus.PAID);
        assertThat(result.replayed()).isFalse();
        then(webhookNotifier).should().send(eq("https://example.com/hooks/orders"), any(OrderWebhookPayload.class));
        assertThat(meterRegistry.counter("shoplite.orders.completed", "outcome", "paid").count()).isEqualTo(1);
    }

    @Test
    void replayOfAPaidOrderDoesNotChargeAgain() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.of(10L));
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L, OrderStatus.PAID));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.replayed()).isTrue();
        then(orderService).should(never()).createPendingOrder(any(), any(), any(), any());
        then(paymentService).shouldHaveNoInteractions();
    }

    @Test
    void declinedPaymentMarksTheOrderFailed() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.empty());
        given(orderService.createPendingOrder(eq(alice), eq(REQUEST), eq("key-1"), anyString())).willReturn(10L);
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L, OrderStatus.PENDING_PAYMENT));
        given(paymentService.charge(anyString(), any(), anyString()))
                .willReturn(new ChargeResponse("pay_2", "DECLINED"));
        given(orderService.markPaymentFailed(10L)).willReturn(order(10L, OrderStatus.PAYMENT_FAILED));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.order().status()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        then(webhookNotifier).shouldHaveNoInteractions();
    }

    @Test
    void providerOutageLeavesTheOrderPending() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.empty());
        given(orderService.createPendingOrder(eq(alice), eq(REQUEST), eq("key-1"), anyString())).willReturn(10L);
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(10L, OrderStatus.PENDING_PAYMENT));
        given(paymentService.charge(anyString(), any(), anyString()))
                .willThrow(new PaymentUnavailableException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> checkoutService.placeOrder(alice, REQUEST, "key-1"))
                .isInstanceOf(PaymentUnavailableException.class);
        then(orderService).should(never()).markPaid(any(), any());
        then(orderService).should(never()).markPaymentFailed(any());
    }

    private static OrderDetails order(Long id, OrderStatus status) {
        return new OrderDetails(id, status, new BigDecimal("19.60"), "EUR", Instant.now(), "alice@shoplite.test",
                "https://example.com/hooks/orders", 0, List.of());
    }

}
