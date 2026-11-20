package com.shoplite.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.shoplite.catalog.CategoryService;

/**
 * Demo for lesson 05: asks the container for the same bean twice and compares.
 * Real code lets Spring inject beans; asking the ApplicationContext directly is only for the demo.
 */
@Component
class ScopeDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ScopeDemo.class);

    private final ApplicationContext context;

    ScopeDemo(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(String... args) {
        boolean sameService = context.getBean(CategoryService.class) == context.getBean(CategoryService.class);
        boolean sameTimer = context.getBean(TaskTimer.class) == context.getBean(TaskTimer.class);
        log.info("singleton CategoryService, same object both times: {}", sameService);
        log.info("prototype TaskTimer, same object both times: {}", sameTimer);
    }

}
