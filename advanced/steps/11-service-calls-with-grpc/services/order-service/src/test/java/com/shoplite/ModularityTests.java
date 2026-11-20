package com.shoplite;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith reads the code (no Spring context, no database) and treats every direct
 * sub-package of com.shoplite as an application module: orders, customers, payments, ...
 */
class ModularityTests {

    // The gRPC classes generated from stock.proto (package com.shoplite.stock.v1) are not a module of
    // this service: they are the catalog's contract. Leave them out of the analysis.
    private static final ApplicationModules modules = ApplicationModules.of(OrderServiceApplication.class,
            JavaClass.Predicates.resideInAPackage("com.shoplite.stock.."));

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
