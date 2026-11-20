package com.shoplite.orders;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoplite.customers.Customer;
import com.shoplite.customers.CustomerHeader;
import com.shoplite.customers.CustomerService;

@Tag(name = "Orders", description = "Place and read the orders of the customer in the Customer-Email header")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;

    public OrderController(OrderService orderService, CustomerService customerService) {
        this.orderService = orderService;
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestHeader(CustomerHeader.NAME) @NotBlank @Email String email,
            @Valid @RequestBody OrderRequest request) {
        Customer customer = customerService.currentCustomer(email);
        OrderResponse created = OrderResponse.from(orderService.placeOrder(customer, request));
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @Operation(summary = "List my orders, newest first")
    @GetMapping
    public PagedModel<OrderSummary> myOrders(@RequestHeader(CustomerHeader.NAME) @NotBlank @Email String email,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Customer customer = customerService.currentCustomer(email);
        return new PagedModel<>(orderService.listForCustomer(customer.getId(), pageable));
    }

    // Two handlers for the same path. Spring MVC picks one by the API-Version header.
    @Operation(summary = "Get one of my orders (API version 1.0)")
    @GetMapping(path = "/{id}", version = "1.0")
    public OrderResponse getOrderV1(@PathVariable Long id,
            @RequestHeader(CustomerHeader.NAME) @NotBlank @Email String email) {
        return OrderResponse.from(findMyOrder(id, email));
    }

    @Operation(summary = "Get one of my orders (API version 2.0)")
    @GetMapping(path = "/{id}", version = "2.0")
    public OrderResponseV2 getOrderV2(@PathVariable Long id,
            @RequestHeader(CustomerHeader.NAME) @NotBlank @Email String email) {
        return OrderResponseV2.from(findMyOrder(id, email));
    }

    private OrderDetails findMyOrder(Long id, String email) {
        Customer customer = customerService.currentCustomer(email);
        return orderService.findForCustomer(id, customer.getId());
    }

}
