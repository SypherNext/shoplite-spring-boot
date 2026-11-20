package com.shoplite.common;

/** Thrown when a request is valid on its own but cannot be applied, for example a reused idempotency key. The API answers 422. */
public class UnprocessableException extends RuntimeException {

    public UnprocessableException(String message) {
        super(message);
    }

}
