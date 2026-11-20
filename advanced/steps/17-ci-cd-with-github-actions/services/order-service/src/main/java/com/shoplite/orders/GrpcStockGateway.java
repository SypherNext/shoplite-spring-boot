package com.shoplite.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;
import com.shoplite.stock.v1.ReleaseRequest;
import com.shoplite.stock.v1.ReserveRequest;
import com.shoplite.stock.v1.StockLine;
import com.shoplite.stock.v1.StockServiceGrpc;

/** The stock API over gRPC: protobuf messages on one long-lived HTTP/2 connection. */
@Component
@ConditionalOnProperty(name = "shoplite.catalog.transport", havingValue = "grpc", matchIfMissing = true)
class GrpcStockGateway implements StockGateway {

    private static final Logger log = LoggerFactory.getLogger(GrpcStockGateway.class);

    // gRPC has no default timeout: without a deadline a call can wait forever.
    private static final long DEADLINE_MILLIS = 2000;

    private final StockServiceGrpc.StockServiceBlockingStub stub;

    GrpcStockGateway(StockServiceGrpc.StockServiceBlockingStub stub) {
        this.stub = stub;
    }

    @Override
    public List<ReservedProduct> reserve(UUID reservationId, List<OrderLineRequest> lines) {
        ReserveRequest request = ReserveRequest.newBuilder()
                .setReservationId(reservationId.toString())
                .addAllLines(lines.stream()
                        .map(line -> StockLine.newBuilder()
                                .setProductId(line.productId())
                                .setQuantity(line.quantity())
                                .build())
                        .toList())
                .build();
        try {
            return stub.withDeadlineAfter(DEADLINE_MILLIS, TimeUnit.MILLISECONDS).reserve(request)
                    .getProductsList().stream()
                    .map(p -> new ReservedProduct(p.getId(), p.getSku(), p.getName(), new BigDecimal(p.getPrice())))
                    .toList();
        } catch (StatusRuntimeException ex) {
            throw translate(reservationId, ex);
        }
    }

    @Override
    public void release(UUID reservationId) {
        try {
            stub.withDeadlineAfter(DEADLINE_MILLIS, TimeUnit.MILLISECONDS)
                    .release(ReleaseRequest.newBuilder().setReservationId(reservationId.toString()).build());
        } catch (StatusRuntimeException ex) {
            throw translate(reservationId, ex);
        }
    }

    /** gRPC status codes instead of HTTP status codes; the mapping is the same idea as in HttpStockGateway. */
    private static RuntimeException translate(UUID reservationId, StatusRuntimeException ex) {
        Status status = ex.getStatus();
        return switch (status.getCode()) {
            case FAILED_PRECONDITION -> new ConflictException(status.getDescription());
            case NOT_FOUND -> new NotFoundException(status.getDescription());
            default -> {
                log.warn("Catalog call for reservation {} failed: {}", reservationId, status);
                yield new CatalogUnavailableException(ex);
            }
        };
    }

}
