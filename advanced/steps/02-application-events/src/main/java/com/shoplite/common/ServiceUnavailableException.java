package com.shoplite.common;

/** Thrown when a service ShopLite depends on is down. The API answers 503 so the client knows to retry later. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
