package com.shoplite.demo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.shoplite.catalog.CategoryService;

/** Demo for lesson 05: logs each moment in a bean's life. Deleted in lesson 07. */
@Component
class BeanLifecycleDemo {

    private static final Logger log = LoggerFactory.getLogger(BeanLifecycleDemo.class);

    private final CategoryService categoryService;

    BeanLifecycleDemo(CategoryService categoryService) {
        this.categoryService = categoryService;
        log.info("constructor: Spring passed in {}", categoryService.getClass().getSimpleName());
    }

    @PostConstruct
    void ready() {
        log.info("@PostConstruct: the bean is ready, it sees {} categories", categoryService.findAll().size());
    }

    @PreDestroy
    void closing() {
        log.info("@PreDestroy: the application context is closing");
    }

}
