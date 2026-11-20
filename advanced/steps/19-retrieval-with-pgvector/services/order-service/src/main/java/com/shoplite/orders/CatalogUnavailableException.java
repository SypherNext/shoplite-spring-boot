package com.shoplite.orders;

import com.shoplite.common.ServiceUnavailableException;

/** The catalog service did not answer, or answered with an error this service cannot fix. Answered as 503. */
public class CatalogUnavailableException extends ServiceUnavailableException {

    public CatalogUnavailableException(Throwable cause) {
        super("The catalog is not available right now. Try again in a few seconds", cause);
    }

}
