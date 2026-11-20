package com.shoplite.common;

/** Thrown when a request clashes with data that already exists. The API answers 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
