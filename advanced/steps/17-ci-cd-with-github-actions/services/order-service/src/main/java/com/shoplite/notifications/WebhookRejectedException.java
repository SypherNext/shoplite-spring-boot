package com.shoplite.notifications;

/** A webhook that can never succeed as it is. Retrying would only waste time. */
public class WebhookRejectedException extends RuntimeException {

    public WebhookRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

}
