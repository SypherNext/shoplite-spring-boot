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
    private final ProductService productService;

    public CatalogStartupReport(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @Override
    public void run(String... args) {
        List<String> names = categoryService.findAll().stream()
                .map(Category::getName)
                .toList();
        log.info("Catalog ready with {} categories {} and {} products",
                names.size(), names, productService.findAll().size());
    }

}
