package com.shoplite.orders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.shoplite.customers.Customer;
import com.shoplite.payments.ChargeResponse;
import com.shoplite.payments.PaymentService;

/**
 * Places an order from start to finish. Deliberately NOT @Transactional, and since the split
 * the steps no longer share one database:
 *   1. catalog service (HTTP): reserve stock for every line, in the catalog's own transaction,
 *      under a reservation id derived from the Idempotency-Key (safe to repeat)
 *   2. transaction here: save a PENDING_PAYMENT order with the reserved products, remember the key
 *      (if this fails, step 1 is undone by a compensating "release" call)
 *   3. no transaction: charge the payment provider
 *   4. transaction here: mark the order PAID, or PAYMENT_FAILED (the catalog puts the stock back
 *      when the PAYMENT_FAILED event reaches it through the outbox and Kafka)
 */
@Service
public class CheckoutService {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final CatalogStock catalogStock;

    public CheckoutService(OrderService orderService, IdempotencyService idempotencyService,
            PaymentService paymentService, CatalogStock catalogStock) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
        this.catalogStock = catalogStock;
    }

    public CheckoutResult placeOrder(Customer customer, OrderRequest request, String idempotencyKey) {
        String requestHash = IdempotencyService.hash(request.fingerprint());
        Optional<Long> earlierOrderId = idempotencyService.findOrderId(customer.getId(), idempotencyKey, requestHash);
        Long orderId = earlierOrderId.orElseGet(() -> createOrder(customer, request, idempotencyKey, requestHash));

        OrderDetails order = orderService.findForCustomer(orderId, customer.getId());
        // Also true on a replay whose first attempt could not reach the provider: try the payment again.
        // The provider sees the same Idempotency-Key ("order-<id>"), so it never charges twice.
        if (order.status() == OrderStatus.PENDING_PAYMENT) {
            order = pay(order);
        }
        return new CheckoutResult(order, earlierOrderId.isPresent());
    }

    private Long createOrder(Customer customer, OrderRequest request, String idempotencyKey, String requestHash) {
        UUID reservationId = reservationId(customer, idempotencyKey, requestHash);
        List<ReservedProduct> reserved = catalogStock.reserve(reservationId, request.lines());
        try {
            return orderService.createPendingOrder(customer, request, reserved, idempotencyKey, requestHash);
        } catch (RuntimeException ex) {
            // No shared transaction can roll back the catalog's change, so undo it by hand.
            catalogStock.release(reservationId);
            throw ex;
        }
    }

    /**
     * The same customer, key and body always give the same reservation id. A client that retries
     * after a timeout therefore meets its first reservation in the catalog instead of a second one.
     */
    static UUID reservationId(Customer customer, String idempotencyKey, String requestHash) {
        String name = "order:" + customer.getId() + ":" + idempotencyKey + ":" + requestHash;
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private OrderDetails pay(OrderDetails order) {
        ChargeResponse charge = paymentService.charge("order-" + order.id(), order.total(), order.currency());
        if (!charge.approved()) {
            return orderService.markPaymentFailed(order.id());
        }
        return orderService.markPaid(order.id(), charge.paymentId());
    }

}
