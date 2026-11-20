package com.shoplite;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * Ready-made tokens for MockMvc tests. jwt() skips signature checks and Keycloak entirely:
 * the request arrives already authenticated with the claims and roles given here.
 */
public final class TestJwts {

    public static JwtRequestPostProcessor customer(String username) {
        return jwt()
                .jwt(token -> token
                        .subject("subject-" + username)
                        .claim("preferred_username", username)
                        .claim("email", username + "@shoplite.test")
                        .claim("name", username))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public static JwtRequestPostProcessor admin() {
        return jwt()
                .jwt(token -> token.subject("subject-olivia").claim("preferred_username", "olivia"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private TestJwts() {
    }

}
