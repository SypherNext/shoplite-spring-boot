package com.shoplite.aot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Tells GraalVM to put shoplite-defaults.properties into native images. A native image only contains
 * the resources someone asked for: Spring asks for application*.properties itself, but it does not know
 * about this file, which every service imports with spring.config.import. Registered in
 * META-INF/spring/aot.factories, so every service that uses this library picks it up.
 */
public class ShopliteDefaultsRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("shoplite-defaults.properties");
    }

}