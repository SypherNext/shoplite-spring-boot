package com.shoplite.orders;

import jakarta.validation.constraints.NotNull;

/**
 * An admin's status change. "version" is the order version the admin last saw:
 * if the order changed since then, the update is refused with 409 instead of
 * silently overwriting someone else's change (a lost update).
 */
public record OrderStatusUpdate(@NotNull OrderStatus status, @NotNull Long version) {
}
