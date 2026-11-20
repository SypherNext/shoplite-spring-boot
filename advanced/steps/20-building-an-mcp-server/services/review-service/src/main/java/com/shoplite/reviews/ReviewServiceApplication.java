package com.shoplite.reviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Lives in com.shoplite.reviews, not com.shoplite: component scanning must not pick up the
 * Spring MVC classes in libs/web-common. This service is WebFlux.
 */
@SpringBootApplication
@ImportRuntimeHints(ReviewRuntimeHints.class)
public class ReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }

}