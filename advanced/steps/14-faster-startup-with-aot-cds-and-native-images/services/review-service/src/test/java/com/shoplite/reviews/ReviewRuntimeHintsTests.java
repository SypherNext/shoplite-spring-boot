package com.shoplite.reviews;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

/** Runs on the JVM in milliseconds, and fails if someone removes a hint the native image depends on. */
class ReviewRuntimeHintsTests {

    @Test
    void offsetDateTimeToInstantIsRegisteredForReflection() {
        RuntimeHints hints = new RuntimeHints();
        new ReviewRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(OffsetDateTime.class, "toInstant"))
                .accepts(hints);
    }

}