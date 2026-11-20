package com.shoplite.reviews;

import java.time.OffsetDateTime;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ReflectionUtils;

/**
 * Reflection the native image needs and cannot find by itself. The PostgreSQL R2DBC driver reads a
 * timestamptz column as OffsetDateTime; Spring's conversion to the record's Instant then calls
 * OffsetDateTime.toInstant() through reflection. On the JVM that just works. In a native image the method
 * is only callable this way if it is registered here: without it, GET /api/products/{id}/reviews was a 500.
 */
class ReviewRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerMethod(
                ReflectionUtils.findMethod(OffsetDateTime.class, "toInstant"), ExecutableMode.INVOKE);
    }

}