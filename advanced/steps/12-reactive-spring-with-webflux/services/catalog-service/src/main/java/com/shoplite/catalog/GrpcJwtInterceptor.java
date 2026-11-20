package com.shoplite.catalog;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.shoplite.stock.v1.StockServiceGrpc;

/**
 * The same rule as the HTTP endpoint: StockService calls need a valid access token with the
 * CUSTOMER role. The token arrives in the "authorization" metadata and is checked by the same
 * JwtDecoder that checks HTTP requests (signature, issuer, audience, expiry).
 * Other gRPC services on this server (reflection, health) stay open.
 */
@Component
@GlobalServerInterceptor
class GrpcJwtInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final JwtDecoder jwtDecoder;

    GrpcJwtInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers,
            ServerCallHandler<Q, R> next) {
        if (!StockServiceGrpc.SERVICE_NAME.equals(call.getMethodDescriptor().getServiceName())) {
            return next.startCall(call, headers);
        }
        String header = headers.get(AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return refuse(call, Status.UNAUTHENTICATED.withDescription("A bearer token is required"));
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring("Bearer ".length()));
            if (!jwt.getClaimAsStringList("roles").contains("CUSTOMER")) {
                return refuse(call, Status.PERMISSION_DENIED.withDescription("The CUSTOMER role is required"));
            }
        } catch (JwtException ex) {
            return refuse(call, Status.UNAUTHENTICATED.withDescription("Invalid token"));
        }
        return next.startCall(call, headers);
    }

    private static <Q, R> ServerCall.Listener<Q> refuse(ServerCall<Q, R> call, Status status) {
        call.close(status, new Metadata());
        return new ServerCall.Listener<>() {
        };
    }

}
