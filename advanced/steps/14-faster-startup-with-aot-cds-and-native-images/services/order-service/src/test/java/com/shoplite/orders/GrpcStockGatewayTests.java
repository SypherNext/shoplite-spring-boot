package com.shoplite.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.shoplite.common.ConflictException;
import com.shoplite.stock.v1.ReserveReply;
import com.shoplite.stock.v1.ReserveRequest;
import com.shoplite.stock.v1.StockServiceGrpc;

/**
 * GrpcStockGateway against a fake catalog on an in-process gRPC server: real stubs, real
 * protobuf and status codes, no network and no Spring.
 */
class GrpcStockGatewayTests {

    private final AtomicReference<String> seenAuthorization = new AtomicReference<>();
    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void stop() {
        SecurityContextHolder.clearContext();
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void reservesAndReadsThePriceAsADecimal() throws Exception {
        GrpcStockGateway gateway = gatewayFor(new StockServiceGrpc.StockServiceImplBase() {
            @Override
            public void reserve(ReserveRequest request, StreamObserver<ReserveReply> responses) {
                responses.onNext(ReserveReply.newBuilder()
                        .addProducts(com.shoplite.stock.v1.ReservedProduct.newBuilder()
                                .setId(4).setSku("NOTE-A5-DOT").setName("Dotted Notebook A5").setPrice("9.80"))
                        .build());
                responses.onCompleted();
            }
        });

        List<ReservedProduct> reserved = gateway.reserve(UUID.randomUUID(), List.of(new OrderLineRequest(4L, 2)));

        assertThat(reserved).containsExactly(
                new ReservedProduct(4L, "NOTE-A5-DOT", "Dotted Notebook A5", new BigDecimal("9.80")));
    }

    @Test
    void failedPreconditionBecomesAConflict() throws Exception {
        GrpcStockGateway gateway = gatewayFor(new StockServiceGrpc.StockServiceImplBase() {
            @Override
            public void reserve(ReserveRequest request, StreamObserver<ReserveReply> responses) {
                responses.onError(Status.FAILED_PRECONDITION
                        .withDescription("Product DESK-STAND-OAK is not for sale").asRuntimeException());
            }
        });

        assertThatThrownBy(() -> gateway.reserve(UUID.randomUUID(), List.of(new OrderLineRequest(6L, 1))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product DESK-STAND-OAK is not for sale");
    }

    @Test
    void aSlowCatalogHitsTheDeadline() throws Exception {
        GrpcStockGateway gateway = gatewayFor(new StockServiceGrpc.StockServiceImplBase() {
            @Override
            public void reserve(ReserveRequest request, StreamObserver<ReserveReply> responses) {
                // Never answers: only the client's 2 second deadline ends the call.
            }
        });

        assertThatThrownBy(() -> gateway.reserve(UUID.randomUUID(), List.of(new OrderLineRequest(4L, 1))))
                .isInstanceOf(CatalogUnavailableException.class)
                .cause()
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED));
    }

    @Test
    void theCustomersTokenTravelsInTheMetadata() throws Exception {
        GrpcStockGateway gateway = gatewayFor(new StockServiceGrpc.StockServiceImplBase() {
            @Override
            public void reserve(ReserveRequest request, StreamObserver<ReserveReply> responses) {
                responses.onNext(ReserveReply.getDefaultInstance());
                responses.onCompleted();
            }
        });
        Jwt jwt = Jwt.withTokenValue("alice-token").header("alg", "none").subject("alice").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        gateway.reserve(UUID.randomUUID(), List.of(new OrderLineRequest(4L, 1)));

        assertThat(seenAuthorization.get()).isEqualTo("Bearer alice-token");
    }

    /** Starts an in-process server with the fake, and builds the gateway on a channel with token relay. */
    private GrpcStockGateway gatewayFor(StockServiceGrpc.StockServiceImplBase fake) throws Exception {
        String name = InProcessServerBuilder.generateName();
        ServerInterceptor recordAuthorization = new ServerInterceptor() {
            @Override
            public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers,
                    ServerCallHandler<Q, R> next) {
                seenAuthorization.set(headers.get(CatalogGrpcConfig.AUTHORIZATION));
                return next.startCall(call, headers);
            }
        };
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(ServerInterceptors.intercept(fake, recordAuthorization)).build().start();
        channel = InProcessChannelBuilder.forName(name)
                .intercept(new CatalogGrpcConfig().bearerTokenRelay()).build();
        return new GrpcStockGateway(StockServiceGrpc.newBlockingStub(channel));
    }

}
