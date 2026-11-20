package com.shoplite.customers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * Finds the customer with this email, creating the row on first use.
     * For now the email doubles as the customer's subject (their stable id). Part 3 swaps it
     * for the id of a signed-in user.
     */
    @Transactional
    public Customer currentCustomer(String email) {
        return customerRepository.findBySubject(email)
                .orElseGet(() -> register(email));
    }

    @Transactional
    public CustomerResponse currentCustomerProfile(String email) {
        return CustomerResponse.from(currentCustomer(email));
    }

    private Customer register(String email) {
        String name = email.split("@")[0];
        Customer customer = customerRepository.save(new Customer(email, email, name));
        log.info("Registered customer {} for subject {}", customer.getId(), customer.getSubject());
        return customer;
    }

}
