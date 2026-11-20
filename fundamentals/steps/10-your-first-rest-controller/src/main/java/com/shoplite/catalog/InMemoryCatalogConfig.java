package com.shoplite.catalog;

import java.math.BigDecimal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the in-memory repositories and fills them with the starting catalog. */
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

    // Spring passes in the CategoryRepository bean built by the method above.
    @Bean
    ProductRepository productRepository(CategoryRepository categories) {
        Category kitchen = categories.findById(1L).orElseThrow();
        Category desk = categories.findById(2L).orElseThrow();
        Category apparel = categories.findById(3L).orElseThrow();

        InMemoryProductRepository products = new InMemoryProductRepository();
        products.save(new Product("Enamel Mug", "MUG-ENAMEL-01", "Speckled enamel camping mug, 350 ml",
                new BigDecimal("12.50"), 40, kitchen));
        products.save(new Product("Steel Water Bottle", "BOTTLE-STEEL-750", "Insulated bottle, keeps drinks cold for 24 hours",
                new BigDecimal("24.90"), 3, kitchen));
        products.save(new Product("Pour-Over Coffee Set", "COFFEE-POUR-SET", "Glass dripper with 100 paper filters",
                new BigDecimal("34.00"), 12, kitchen));
        products.save(new Product("Dotted Notebook A5", "NOTE-A5-DOT", "160 pages of 100 gsm dotted paper",
                new BigDecimal("9.80"), 75, desk));
        products.save(new Product("Gel Pen Pack", "PEN-GEL-5PK", "Five black gel pens, 0.5 mm",
                new BigDecimal("6.40"), 2, desk));
        Product stand = new Product("Monitor Stand", "DESK-STAND-OAK", "Solid oak monitor riser",
                new BigDecimal("49.00"), 0, desk);
        stand.setActive(false);
        products.save(stand);
        products.save(new Product("Logo T-Shirt", "TEE-LOGO-M", "Organic cotton T-shirt, size M",
                new BigDecimal("19.00"), 25, apparel));
        products.save(new Product("Zip Hoodie", "HOODIE-ZIP-L", "Heavyweight zip hoodie, size L",
                new BigDecimal("54.00"), 5, apparel));
        return products;
    }

}
