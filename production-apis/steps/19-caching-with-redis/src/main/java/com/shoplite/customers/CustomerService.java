package com.shoplite.customers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Finds the customer behind an access token, creating the row on first use.
     * Keycloak owns the account (password, email); ShopLite only keeps what orders need.
     */
    @Transactional
    public Customer currentCustomer(Jwt jwt) {
        return customerRepository.findBySubject(jwt.getSubject())
                .orElseGet(() -> register(jwt));
    }

    @Transactional
    public CustomerResponse currentCustomerProfile(Jwt jwt) {
        return CustomerResponse.from(currentCustomer(jwt));
    }

    private Customer register(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        Customer customer = customerRepository.save(new Customer(jwt.getSubject(),
                email == null ? "unknown" : email,
                name == null ? jwt.getClaimAsString("preferred_username") : name));
        log.info("Registered customer {} for subject {}", customer.getId(), customer.getSubject());
        return customer;
    }

}
