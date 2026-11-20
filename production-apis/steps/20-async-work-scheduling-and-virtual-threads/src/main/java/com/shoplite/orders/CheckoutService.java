package com.shoplite.orders;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shoplite.customers.Customer;
import com.shoplite.notifications.OrderWebhookNotifier;
import com.shoplite.notifications.OrderWebhookPayload;
import com.shoplite.payments.ChargeResponse;
import com.shoplite.payments.PaymentService;

/**
 * Places an order from start to finish. Deliberately NOT @Transactional:
 *   1. transaction: reserve stock, save a PENDING_PAYMENT order, remember the key
 *   2. no transaction: charge the payment provider (may take seconds, may be retried)
 *   3. transaction: mark the order PAID, or PAYMENT_FAILED and put the stock back
 *   4. after the response: send the webhook (@Async)
 */
@Service
public class CheckoutService {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final OrderWebhookNotifier webhookNotifier;

    public CheckoutService(OrderService orderService, IdempotencyService idempotencyService,
            PaymentService paymentService, OrderWebhookNotifier webhookNotifier) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
        this.webhookNotifier = webhookNotifier;
    }

    public CheckoutResult placeOrder(Customer customer, OrderRequest request, String idempotencyKey) {
        String requestHash = IdempotencyService.hash(request.fingerprint());
        Optional<Long> earlierOrderId = idempotencyService.findOrderId(customer.getId(), idempotencyKey, requestHash);
        Long orderId = earlierOrderId.orElseGet(
                () -> orderService.createPendingOrder(customer, request, idempotencyKey, requestHash));

        OrderDetails order = orderService.findForCustomer(orderId, customer.getId());
        // Also true on a replay whose first attempt could not reach the provider: try the payment again.
        // The provider sees the same Idempotency-Key ("order-<id>"), so it never charges twice.
        if (order.status() == OrderStatus.PENDING_PAYMENT) {
            order = pay(order);
        }
        return new CheckoutResult(order, earlierOrderId.isPresent());
    }

    private OrderDetails pay(OrderDetails order) {
        ChargeResponse charge = paymentService.charge("order-" + order.id(), order.total(), order.currency());
        if (!charge.approved()) {
            return orderService.markPaymentFailed(order.id());
        }
        OrderDetails paid = orderService.markPaid(order.id(), charge.paymentId());
        if (paid.notifyUrl() != null) {
            webhookNotifier.send(paid.notifyUrl(),
                    new OrderWebhookPayload("order.paid", paid.id(), paid.status().name(), paid.total(), paid.currency()));
        }
        return paid;
    }

}
