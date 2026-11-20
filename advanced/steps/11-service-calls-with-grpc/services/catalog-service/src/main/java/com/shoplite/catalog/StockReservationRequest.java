package com.shoplite.catalog;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * POST /internal/stock/reservations: take these quantities out of stock, all or nothing.
 * The caller chooses the reservationId, so sending the same request again is harmless.
 */
record StockReservationRequest(@NotNull UUID reservationId, @NotEmpty List<@Valid StockLine> lines) {
}
