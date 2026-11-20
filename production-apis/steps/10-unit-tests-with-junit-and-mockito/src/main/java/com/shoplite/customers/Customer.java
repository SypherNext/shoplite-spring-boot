package com.shoplite.customers;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A shopper. Created the first time someone with a valid token uses the orders API. */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The token's "sub" claim: Keycloak's id for this user.
    @Column(nullable = false, unique = true, length = 100)
    private String subject;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Customer() {
    }

    public Customer(String subject, String email, String fullName) {
        this.subject = subject;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
