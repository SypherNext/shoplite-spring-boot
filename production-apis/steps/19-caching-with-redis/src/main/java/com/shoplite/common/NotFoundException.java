package com.shoplite.common;

/** Thrown when a requested record does not exist. The API answers 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super(resource + " " + id + " was not found");
    }

}
