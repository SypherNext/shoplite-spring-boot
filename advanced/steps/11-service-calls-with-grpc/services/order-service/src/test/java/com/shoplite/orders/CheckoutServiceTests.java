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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.shoplite.customers.Customer;
import com.shoplite.payments.ChargeResponse;
import com.shoplite.payments.PaymentService;

/** Unit tests with Mockito: CheckoutService runs for real, everything it talks to is a mock. */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTests {

    @Mock
    OrderService orderService;
    @Mock
    IdempotencyService idempotencyService;
    @Mock
    PaymentService paymentService;
    @Mock
    CatalogStock catalogStock;

    CheckoutService checkoutService;
    Customer alice = new Customer("sub-alice", "alice@shoplite.test", "Alice");

    private static final List<OrderLineRequest> LINES = List.of(new OrderLineRequest(4L, 2));
    private static final OrderRequest REQUEST = new OrderRequest(LINES, null);
    private static final List<ReservedProduct> RESERVED =
            List.of(new ReservedProduct(4L, "NOTE-A5-DOT", "Dotted Notebook A5", new BigDecimal("9.80")));

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(orderService, idempotencyService, paymentService, catalogStock);
        ReflectionTestUtils.setField(alice, "id", 1L);
    }

    @Test
    void reservesStockThenSavesAndPays() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.empty());
        given(catalogStock.reserve(any(UUID.class), eq(LINES))).willReturn(RESERVED);
        given(orderService.createPendingOrder(eq(alice), eq(REQUEST), eq(RESERVED), eq("key-1"), anyString()))
                .willReturn(10L);
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(OrderStatus.PENDING_PAYMENT));
        given(paymentService.charge("order-10", new BigDecimal("19.60"), "EUR"))
                .willReturn(new ChargeResponse("pay_1", "APPROVED"));
        given(orderService.markPaid(10L, "pay_1")).willReturn(order(OrderStatus.PAID));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.order().status()).isEqualTo(OrderStatus.PAID);
        then(catalogStock).should(never()).release(any());
    }

    @Test
    void releasesTheStockWhenTheOrderCannotBeSaved() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.empty());
        given(catalogStock.reserve(any(UUID.class), eq(LINES))).willReturn(RESERVED);
        given(orderService.createPendingOrder(any(), any(), any(), any(), any()))
                .willThrow(new DataIntegrityViolationException("value too long"));

        assertThatThrownBy(() -> checkoutService.placeOrder(alice, REQUEST, "key-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        then(catalogStock).should().release(CheckoutService.reservationId(alice, "key-1",
                IdempotencyService.hash(REQUEST.fingerprint())));
        then(paymentService).shouldHaveNoInteractions();
    }

    @Test
    void replayOfAPaidOrderTouchesNeitherCatalogNorPayment() {
        given(idempotencyService.findOrderId(eq(1L), eq("key-1"), anyString())).willReturn(Optional.of(10L));
        given(orderService.findForCustomer(10L, 1L)).willReturn(order(OrderStatus.PAID));

        CheckoutResult result = checkoutService.placeOrder(alice, REQUEST, "key-1");

        assertThat(result.replayed()).isTrue();
        then(catalogStock).shouldHaveNoInteractions();
        then(paymentService).shouldHaveNoInteractions();
    }

    private static OrderDetails order(OrderStatus status) {
        return new OrderDetails(10L, status, new BigDecimal("19.60"), "EUR", Instant.now(), "alice@shoplite.test",
                null, 0, List.of());
    }

}
