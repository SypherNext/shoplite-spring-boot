package com.shoplite.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.catalog.Product;
import com.shoplite.catalog.ProductService;
import com.shoplite.common.NotFoundException;
import com.shoplite.customers.Customer;
import com.shoplite.shop.ShopProperties;

/** Every public method here is one database transaction. */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // ShopLite has no payment provider yet (lesson 17 adds one), so every order counts as paid at once.
    private static final String NOT_CHARGED = "not-charged";

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final ShopProperties shopProperties;

    public OrderService(OrderRepository orderRepository, ProductService productService,
            ShopProperties shopProperties) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.shopProperties = shopProperties;
    }

    /**
     * Builds the order line by line, takes the items out of stock, and saves everything together.
     * All or nothing: if any line is out of stock, nothing is saved and no stock is taken.
     */
    @Transactional
    public OrderResponse placeOrder(Customer customer, OrderRequest request) {
        Order order = new Order(customer, shopProperties.currency(), null);
        for (OrderLineRequest line : request.lines()) {
            Product product = productService.reserveStock(line.productId(), line.quantity());
            order.addLine(product, line.quantity());
        }
        order.markPaid(NOT_CHARGED);
        // One save: cascade = ALL on Order.lines inserts the lines too.
        Order saved = orderRepository.save(order);
        log.info("Order {} created for customer {} with total {} {}", saved.getId(), customer.getId(),
                saved.getTotal(), saved.getCurrency());
        return OrderResponse.from(saved);
    }

    /** A customer may only see their own orders. Someone else's order is reported as not found. */
    @Transactional(readOnly = true)
    public OrderResponse findForCustomer(Long orderId, Long customerId) {
        Order order = getOrder(orderId);
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new NotFoundException("Order", orderId);
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> listForCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderRow> listByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusWithCustomer(status, pageable).map(AdminOrderRow::from);
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findWithCustomerAndLinesById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }

}
