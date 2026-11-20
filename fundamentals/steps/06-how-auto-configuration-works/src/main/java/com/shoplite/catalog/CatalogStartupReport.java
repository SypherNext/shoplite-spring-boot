package com.shoplite.catalog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Prints one line about the catalog once the application has started.
 * Switch it off with shoplite.startup-report.enabled=false.
 */
@Component
@ConditionalOnBooleanProperty(name = "shoplite.startup-report.enabled", matchIfMissing = true)
public class CatalogStartupReport implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogStartupReport.class);

    private final CategoryService categoryService;

    public CatalogStartupReport(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public void run(String... args) {
        List<String> names = categoryService.findAll().stream()
                .map(Category::getName)
                .toList();
        log.info("Catalog ready with {} categories: {}", names.size(), names);
    }

}
