package com.shoplite.catalog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the in-memory repository and fills it with the starting categories. */
@Configuration(proxyBeanMethods = false)
public class InMemoryCatalogConfig {

    @Bean
    CategoryRepository categoryRepository() {
        InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
        categories.save(new Category("Kitchen", "Mugs, bottles and things for your coffee break"));
        categories.save(new Category("Desk", "Notebooks, pens and desk accessories"));
        categories.save(new Category("Apparel", "T-shirts and hoodies"));
        return categories;
    }

}
