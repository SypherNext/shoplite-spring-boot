package com.shoplite.orders;

import java.util.List;
import java.util.UUID;

/**
 * How the order service talks to the catalog's stock API. Two implementations: gRPC (the default)
 * and HTTP (shoplite.catalog.transport=http). Both report failures with this service's own
 * exceptions: ConflictException, NotFoundException or CatalogUnavailableException.
 */
public interface StockGateway {

    List<ReservedProduct> reserve(UUID reservationId, List<OrderLineRequest> lines);

    void release(UUID reservationId);

}
