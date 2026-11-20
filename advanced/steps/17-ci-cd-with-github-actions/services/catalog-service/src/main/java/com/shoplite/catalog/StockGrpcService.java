package com.shoplite.catalog;

import java.util.List;
import java.util.UUID;

import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import com.shoplite.stock.v1.ReleaseReply;
import com.shoplite.stock.v1.ReleaseRequest;
import com.shoplite.stock.v1.ReserveReply;
import com.shoplite.stock.v1.ReserveRequest;
import com.shoplite.stock.v1.StockServiceGrpc;

/**
 * The stock API over gRPC. It only translates between protobuf messages and the catalog's own
 * types; StockReservationService does the work, exactly as for the HTTP endpoint.
 * Exceptions are turned into gRPC status codes by StockGrpcExceptionHandler.
 */
@GrpcService
class StockGrpcService extends StockServiceGrpc.StockServiceImplBase {

    private final StockReservationService reservations;

    StockGrpcService(StockReservationService reservations) {
        this.reservations = reservations;
    }

    @Override
    public void reserve(ReserveRequest request, StreamObserver<ReserveReply> responses) {
        List<StockLine> lines = request.getLinesList().stream()
                .map(line -> new StockLine(line.getProductId(), line.getQuantity()))
                .toList();
        List<ReservedProduct> reserved = reservations.reserve(UUID.fromString(request.getReservationId()), lines);
        responses.onNext(ReserveReply.newBuilder()
                .addAllProducts(reserved.stream()
                        .map(p -> com.shoplite.stock.v1.ReservedProduct.newBuilder()
                                .setId(p.id())
                                .setSku(p.sku())
                                .setName(p.name())
                                .setPrice(p.price().toPlainString())
                                .build())
                        .toList())
                .build());
        responses.onCompleted();
    }

    @Override
    public void release(ReleaseRequest request, StreamObserver<ReleaseReply> responses) {
        reservations.release(UUID.fromString(request.getReservationId()));
        responses.onNext(ReleaseReply.getDefaultInstance());
        responses.onCompleted();
    }

}
