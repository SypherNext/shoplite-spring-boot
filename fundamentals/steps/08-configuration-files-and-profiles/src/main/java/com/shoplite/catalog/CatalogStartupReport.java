package com.shoplite.catalog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Prints a short report about the catalog once the application has started.
 * Switch it off with shoplite.startup-report.enabled=false.
 */
@Component
@ConditionalOnBooleanProperty(name = "shoplite.startup-report.enabled", matchIfMissing = true)
public class CatalogStartupReport implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogStartupReport.class);

    private final CategoryService categoryService;
    private final ProductService productService;
    private final String shopName;

    public CatalogStartupReport(CategoryService categoryService, ProductService productService,
            @Value("${shoplite.shop.name}") String shopName) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.shopName = shopName;
    }

    @Override
    public void run(String... args) {
        List<String> names = categoryService.findAll().stream()
                .map(Category::getName)
                .toList();
        log.info("{} catalog ready with {} categories {} and {} products",
                shopName, names.size(), names, productService.findAll().size());
        List<String> lowStock = productService.findLowStock().stream()
                .map(Product::getSku)
                .toList();
        // DEBUG: printed by the dev profile, hidden by the prod profile.
        log.debug("Low on stock: {}", lowStock);
    }

}
