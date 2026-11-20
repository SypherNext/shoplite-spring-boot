package com.shoplite.orders;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoplite.common.ProblemExamples;
import com.shoplite.customers.Customer;
import com.shoplite.customers.CustomerService;

@Tag(name = "Orders", description = "Place and read your own orders (CUSTOMER)")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final CustomerService customerService;

    public OrderController(CheckoutService checkoutService, OrderService orderService,
            CustomerService customerService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
        this.customerService = customerService;
    }

    @Operation(summary = "Place an order",
            description = "Send a new unique Idempotency-Key with every new order. Sending the same key "
                    + "and body again returns the first order (200 with Idempotent-Replayed: true) "
                    + "instead of creating and charging a second one.")
    @ApiResponse(responseCode = "201", description = "Order created. Check status: PAID or PAYMENT_FAILED")
    @ApiResponse(responseCode = "200", description = "Replay of an earlier request with the same key")
    @ApiResponse(responseCode = "422", description = "The key was already used with a different body",
            content = @Content(mediaType = "application/problem+json",
                    examples = @ExampleObject(value = ProblemExamples.KEY_REUSED)))
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Parameter(description = "A unique value per new order, for example a UUID")
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Customer customer = customerService.currentCustomer(jwt);
        CheckoutResult result = checkoutService.placeOrder(customer, request, idempotencyKey);
        OrderResponse body = OrderResponse.from(result.order());
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/orders/" + body.id())).body(body);
    }

    @Operation(summary = "List my orders, newest first")
    @GetMapping
    public PagedModel<OrderSummary> myOrders(@AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Customer customer = customerService.currentCustomer(jwt);
        return new PagedModel<>(orderService.listForCustomer(customer.getId(), pageable));
    }

    // Two handlers for the same path. Spring MVC picks one by the API-Version header.
    @Operation(summary = "Get one of my orders (API version 1.0)")
    @GetMapping(path = "/{id}", version = "1.0")
    public OrderResponse getOrderV1(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return OrderResponse.from(findMyOrder(id, jwt));
    }

    @Operation(summary = "Get one of my orders (API version 2.0)")
    @GetMapping(path = "/{id}", version = "2.0")
    public OrderResponseV2 getOrderV2(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return OrderResponseV2.from(findMyOrder(id, jwt));
    }

    private OrderDetails findMyOrder(Long id, Jwt jwt) {
        Customer customer = customerService.currentCustomer(jwt);
        return orderService.findForCustomer(id, customer.getId());
    }

}
