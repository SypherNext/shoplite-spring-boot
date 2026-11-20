package com.shoplite;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;

/**
 * Ready-made users for MockMvc tests. user() skips the password check: the request arrives
 * already signed in, with the roles given here.
 */
public final class TestUsers {

    public static UserRequestPostProcessor customer(String name) {
        return user(name + "@shoplite.test").roles("CUSTOMER");
    }

    public static UserRequestPostProcessor admin() {
        return user("olivia@shoplite.test").roles("ADMIN");
    }

    private TestUsers() {
    }

}
