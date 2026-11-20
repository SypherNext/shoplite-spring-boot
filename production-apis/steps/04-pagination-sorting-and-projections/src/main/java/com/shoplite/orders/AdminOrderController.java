package com.shoplite.orders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Open to anyone until Part 3 adds security. Never deploy an admin endpoint like this.
@Tag(name = "Admin: orders", description = "Every customer's orders")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "List orders in one status, oldest first")
    @GetMapping
    public PagedModel<AdminOrderRow> listByStatus(@RequestParam(defaultValue = "PAID") OrderStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return new PagedModel<>(orderService.listByStatus(status, pageable));
    }

}
