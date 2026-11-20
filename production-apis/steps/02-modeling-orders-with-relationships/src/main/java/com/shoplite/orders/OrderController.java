package com.shoplite.orders;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
        OrderResponse created = orderService.placeOrder(customer, request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @GetMapping
    public List<OrderResponse> myOrders(@RequestHeader(CustomerHeader.NAME) @NotBlank @Email String email) {
        return orderService.listForCustomer(customerService.currentCustomer(email).getId());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id,
            @RequestHeader(CustomerHeader.NAME) @NotBlank @Email String email) {
        return orderService.findForCustomer(id, customerService.currentCustomer(email).getId());
    }

}
