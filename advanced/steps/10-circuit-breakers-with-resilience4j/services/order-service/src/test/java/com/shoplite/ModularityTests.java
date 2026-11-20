package com.shoplite;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith reads the code (no Spring context, no database) and treats every direct
 * sub-package of com.shoplite as an application module: orders, customers, payments, ...
 */
class ModularityTests {

    private static final ApplicationModules modules = ApplicationModules.of(OrderServiceApplication.class);

    /** Fails the build on a dependency cycle between modules, or on access to another module's internals. */
    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    /** Prints each module with its public types and dependencies (see the test report's standard output). */
    @Test
    void printModules() {
        modules.forEach(System.out::println);
    }

    /** Writes PlantUML diagrams and a module canvas to build/spring-modulith-docs. */
    @Test
    void writeDocumentation() {
        new Documenter(modules).writeDocumentation();
    }

}
