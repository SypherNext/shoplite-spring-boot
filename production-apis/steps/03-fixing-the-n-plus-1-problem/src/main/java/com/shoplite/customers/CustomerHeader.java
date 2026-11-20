package com.shoplite.customers;

/**
 * Until sign-in arrives in Part 3, a request says which customer it speaks for with this header.
 * Anyone can send any email here, so it is a stand-in for development, never something to ship.
 */
public final class CustomerHeader {

    public static final String NAME = "Customer-Email";

    private CustomerHeader() {
    }

}
