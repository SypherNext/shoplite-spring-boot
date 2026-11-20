package com.shoplite.orders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shoplite.common.ProblemExamples;

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

    @Operation(summary = "Ship or cancel a paid order",
            description = "Send the version you last read. If the order changed since then, you get 409.")
    @ApiResponse(responseCode = "200", description = "Updated. The response carries the new version")
    @ApiResponse(responseCode = "409", description = "Stale version, or a move the order does not allow",
            content = @Content(mediaType = "application/problem+json",
                    examples = @ExampleObject(value = ProblemExamples.STALE_VERSION)))
    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdate update) {
        return OrderResponse.from(orderService.changeStatus(id, update));
    }

}
