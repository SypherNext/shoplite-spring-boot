package com.shoplite.orders;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.common.NotFoundException;
import com.shoplite.customers.Customer;
import com.shoplite.shop.ShopProperties;

/**
 * Every method here is one database transaction. Network calls (payment, webhooks) happen
 * outside, in CheckoutService and in event listeners, so a slow provider never holds a
 * database connection or row locks.
 *
 * When an order changes state, this service publishes an OrderEvent. It does not know or
 * care who listens: notifications and metrics subscribe on their own.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final ShopProperties shopProperties;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orderRepository, IdempotencyService idempotencyService,
            ShopProperties shopProperties, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.idempotencyService = idempotencyService;
        this.shopProperties = shopProperties;
        this.events = events;
    }

    /**
     * Saves the order as PENDING_PAYMENT, with one line per product the catalog reserved,
     * and remembers the Idempotency-Key. The stock was already taken by the catalog service.
     */
    @Transactional
    public Long createPendingOrder(Customer customer, OrderRequest request, List<ReservedProduct> reserved,
            String idempotencyKey, String requestHash) {
        Order order = new Order(customer, shopProperties.currency(), request.notifyUrl());
        for (int i = 0; i < request.lines().size(); i++) {
            order.addLine(reserved.get(i), request.lines().get(i).quantity());
        }
        Order saved = orderRepository.save(order);
        idempotencyService.remember(customer.getId(), idempotencyKey, requestHash, saved.getId());
        log.info("Order {} created for customer {} with total {} {}", saved.getId(), customer.getId(),
                saved.getTotal(), saved.getCurrency());
        return saved.getId();
    }

    @Transactional
    public OrderDetails markPaid(Long orderId, String paymentId) {
        Order order = getOrder(orderId);
        order.markPaid(paymentId);
        OrderDetails details = saveAndDescribe(order);
        // Published inside the transaction. Listeners decide whether they run now
        // (@EventListener) or only after a successful commit (@TransactionalEventListener).
        events.publishEvent(OrderPaid.of(order));
        return details;
    }

    /** The payment was declined: the order fails. The catalog puts the stock back when it hears the event. */
    @Transactional
    public OrderDetails markPaymentFailed(Long orderId) {
        Order order = getOrder(orderId);
        order.markPaymentFailed();
        OrderDetails details = saveAndDescribe(order);
        events.publishEvent(OrderPaymentFailed.of(order));
        return details;
    }

    /** A customer may only see their own orders. Someone else's order is reported as not found. */
    @Transactional(readOnly = true)
    public OrderDetails findForCustomer(Long orderId, Long customerId) {
        Order order = getOrder(orderId);
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new NotFoundException("Order", orderId);
        }
        return OrderDetails.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> listForCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    // Method security: checked again here even though the URL rule already requires ADMIN.
    // If someone later calls this method from a new endpoint, the rule still holds.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AdminOrderRow> listByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusWithCustomer(status, pageable).map(AdminOrderRow::from);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public OrderDetails changeStatus(Long orderId, OrderStatusUpdate update) {
        Order order = getOrder(orderId);
        // The admin edited an older copy: refuse instead of overwriting the newer data.
        if (order.getVersion() != update.version()) {
            throw new OptimisticLockingFailureException("Order " + orderId + " is at version "
                    + order.getVersion() + " but the request was based on version " + update.version());
        }
        order.changeStatus(update.status());
        if (update.status() == OrderStatus.CANCELLED) {
            events.publishEvent(OrderCancelled.of(order));
        }
        log.info("Order {} moved to {}", orderId, update.status());
        return saveAndDescribe(order);
    }

    // Flush now so the response carries the new @Version value that the UPDATE produced.
    private OrderDetails saveAndDescribe(Order order) {
        return OrderDetails.from(orderRepository.saveAndFlush(order));
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findWithCustomerAndLinesById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }

}
