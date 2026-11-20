package com.shoplite.orders;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shoplite.customers.Customer;

/**
 * Places an order at most once per Idempotency-Key. Deliberately NOT @Transactional:
 * the key lookup and the order creation are each their own transaction.
 */
@Service
public class CheckoutService {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public CheckoutService(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    public CheckoutResult placeOrder(Customer customer, OrderRequest request, String idempotencyKey) {
        String requestHash = IdempotencyService.hash(request.fingerprint());
        Optional<Long> earlierOrderId = idempotencyService.findOrderId(customer.getId(), idempotencyKey, requestHash);
        Long orderId = earlierOrderId.orElseGet(
                () -> orderService.createOrder(customer, request, idempotencyKey, requestHash));
        OrderDetails order = orderService.findForCustomer(orderId, customer.getId());
        return new CheckoutResult(order, earlierOrderId.isPresent());
    }

}
