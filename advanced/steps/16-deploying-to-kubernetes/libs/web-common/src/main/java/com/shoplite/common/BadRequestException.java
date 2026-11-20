package com.shoplite.common;

/** Thrown when a request is well-formed JSON but asks for something the API does not support. The API answers 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

}
