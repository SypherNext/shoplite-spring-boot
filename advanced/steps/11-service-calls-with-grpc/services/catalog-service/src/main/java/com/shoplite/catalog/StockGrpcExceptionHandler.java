package com.shoplite.catalog;

import io.grpc.Status;
import io.grpc.StatusException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;

/**
 * The gRPC twin of ApiExceptionHandler: the catalog's exceptions become gRPC status codes.
 * Returning null leaves an exception to Spring gRPC's default (status UNKNOWN).
 */
@Component
class StockGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable exception) {
        if (exception instanceof ConflictException) {
            return Status.FAILED_PRECONDITION.withDescription(exception.getMessage()).asException();
        }
        if (exception instanceof NotFoundException) {
            return Status.NOT_FOUND.withDescription(exception.getMessage()).asException();
        }
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asException();
        }
        return null;
    }

}
