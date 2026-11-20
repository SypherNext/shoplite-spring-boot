package com.shoplite.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Plain unit tests: what the password encoder does with the stored hashes. */
class InMemoryUsersConfigTests {

    private final PasswordEncoder encoder = new InMemoryUsersConfig().passwordEncoder();

    @Test
    void theStoredHashMatchesTheRightPasswordOnly() {
        assertThat(encoder.matches("alice-dev-pass", InMemoryUsersConfig.ALICE_HASH)).isTrue();
        assertThat(encoder.matches("alice-dev-pass2", InMemoryUsersConfig.ALICE_HASH)).isFalse();
    }

    @Test
    void theSamePasswordHashesDifferentlyEveryTime() {
        String first = encoder.encode("alice-dev-pass");
        String second = encoder.encode("alice-dev-pass");

        assertThat(first).startsWith("{bcrypt}$2a$10$").isNotEqualTo(second);
        assertThat(encoder.matches("alice-dev-pass", first)).isTrue();
        assertThat(encoder.matches("alice-dev-pass", second)).isTrue();
    }

}
