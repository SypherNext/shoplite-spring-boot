package com.shoplite.orders;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.shoplite.stock.v1.StockServiceGrpc;

/**
 * A blocking stub for the catalog's StockService, on the channel named "catalog"
 * (spring.grpc.client.channel.catalog.target), plus token relay for every gRPC call.
 */
@Configuration(proxyBeanMethods = false)
@ImportGrpcClients(target = "catalog", types = StockServiceGrpc.StockServiceBlockingStub.class)
class CatalogGrpcConfig {

    static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /** Copies the current customer's access token into the call's metadata (gRPC's headers). */
    @Bean
    @GlobalClientInterceptor
    ClientInterceptor bearerTokenRelay() {
        return new ClientInterceptor() {
            @Override
            public <Q, R> ClientCall<Q, R> interceptCall(MethodDescriptor<Q, R> method, CallOptions options,
                    Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
                    @Override
                    public void start(Listener<R> listener, Metadata headers) {
                        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
                            headers.put(AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue());
                        }
                        super.start(listener, headers);
                    }
                };
            }
        };
    }

}
