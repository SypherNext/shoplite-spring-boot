package com.shoplite.orders;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
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

    @GetMapping
    public List<AdminOrderRow> listByStatus(@RequestParam(defaultValue = "PAID") OrderStatus status) {
        return orderService.listByStatus(status);
    }

}
