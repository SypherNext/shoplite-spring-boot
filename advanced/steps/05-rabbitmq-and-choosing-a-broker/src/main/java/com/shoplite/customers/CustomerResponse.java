package com.shoplite.customers;

import java.time.Instant;

public record CustomerResponse(Long id, String email, String fullName, Instant createdAt) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getFullName(),
                customer.getCreatedAt());
    }

}
