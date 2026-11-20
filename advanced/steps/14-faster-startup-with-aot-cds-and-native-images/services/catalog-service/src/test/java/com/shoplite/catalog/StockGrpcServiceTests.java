package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.shoplite.TestcontainersConfiguration;
import com.shoplite.stock.v1.ReserveReply;
import com.shoplite.stock.v1.ReserveRequest;
import com.shoplite.stock.v1.StockLine;
import com.shoplite.stock.v1.StockServiceGrpc;

/**
 * The real catalog with its gRPC server on a free port (port 0), called through a real channel.
 * The JwtDecoder is a mock, so the test decides which tokens are valid.
 */
@SpringBootTest(properties = "spring.grpc.server.port=0")
@Import(TestcontainersConfiguration.class)
class StockGrpcServiceTests {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @LocalGrpcServerPort
    int port;

    @MockitoBean
    JwtDecoder jwtDecoder;

    ManagedChannel channel;

    @BeforeEach
    void setUp() {
        channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        given(jwtDecoder.decode("customer-token")).willReturn(Jwt.withTokenValue("customer-token")
                .header("alg", "none").subject("alice").claim("roles", List.of("CUSTOMER")).build());
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
    }

    @Test
    void reservesStockOverGrpc() {
        ReserveReply reply = stub("customer-token").reserve(request(1, 1));

        assertThat(reply.getProductsList()).singleElement().satisfies(product -> {
            assertThat(product.getSku()).isEqualTo("MUG-ENAMEL-01");
            assertThat(product.getPrice()).isEqualTo("12.50");
        });
    }

    @Test
    void aProductNotForSaleIsAFailedPrecondition() {
        assertThatThrownBy(() -> stub("customer-token").reserve(request(6, 1)))
                .isInstanceOfSatisfying(StatusRuntimeException.class, ex -> {
                    assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
                    assertThat(ex.getStatus().getDescription()).isEqualTo("Product DESK-STAND-OAK is not for sale");
                });
    }

    @Test
    void aCallWithoutATokenIsUnauthenticated() {
        assertThatThrownBy(() -> StockServiceGrpc.newBlockingStub(channel).reserve(request(1, 1)))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    private StockServiceGrpc.StockServiceBlockingStub stub(String token) {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Bearer " + token);
        return StockServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    private static ReserveRequest request(long productId, int quantity) {
        return ReserveRequest.newBuilder()
                .setReservationId(UUID.randomUUID().toString())
                .addLines(StockLine.newBuilder().setProductId(productId).setQuantity(quantity))
                .build();
    }

}
