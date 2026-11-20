package com.shoplite.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Three users kept in memory, for learning. Only BCrypt hashes are stored, never the passwords.
 * Development only: the passwords are alice-dev-pass, bob-dev-pass and olivia-dev-pass.
 * A real app keeps its users in a database or, as from lesson 15, in an identity provider.
 */
@Configuration(proxyBeanMethods = false)
public class InMemoryUsersConfig {

    static final String ALICE_HASH = "{bcrypt}$2a$10$HEhj4kw92ImrenEs0sEV0uj.MaTcjb4lsD4xBxWoyONIWNCTkD0T6";
    static final String BOB_HASH = "{bcrypt}$2a$10$mn29hvXvuOSGKH6FyD4Eg.hFo5gV24mUrwptVIB.d74QIW/QYY43.";
    static final String OLIVIA_HASH = "{bcrypt}$2a$10$7fTtxKxg8tjgqX1KdQGVGO4.dp75LaBfoHXGxzBnI6vByDLYXxL3u";

    /** Reads the {id} prefix of a stored hash to pick the algorithm. New hashes use BCrypt. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // The username is the customer's email, so CustomerService can find or create the customer.
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                user("alice@shoplite.test", ALICE_HASH, "CUSTOMER"),
                user("bob@shoplite.test", BOB_HASH, "CUSTOMER"),
                user("olivia@shoplite.test", OLIVIA_HASH, "ADMIN"));
    }

    // roles("CUSTOMER") stores the authority ROLE_CUSTOMER; hasRole("CUSTOMER") checks for it.
    private static UserDetails user(String username, String passwordHash, String role) {
        return User.withUsername(username).password(passwordHash).roles(role).build();
    }

}
