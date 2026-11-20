package com.shoplite.orders;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.catalog.Product;
import com.shoplite.catalog.ProductService;
import com.shoplite.common.NotFoundException;
import com.shoplite.customers.Customer;
import com.shoplite.shop.ShopProperties;

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

    /** Builds the order line by line, takes the items out of stock, and saves everything together. */
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
    public List<OrderResponse> listForCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOrderRow> listByStatus(OrderStatus status) {
        return orderRepository.findByStatusWithCustomer(status).stream()
                .map(AdminOrderRow::from)
                .toList();
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findWithCustomerAndLinesById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }

}
