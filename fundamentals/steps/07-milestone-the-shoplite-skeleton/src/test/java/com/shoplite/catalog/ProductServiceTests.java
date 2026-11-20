package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductServiceTests {

    private final Category kitchen = new Category("Kitchen", null);

    @Test
    void listsProductsSortedByName() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        repository.save(new Product("Teapot", "TEAPOT-1", null, new BigDecimal("30.00"), 4, kitchen));
        repository.save(new Product("Mug", "MUG-1", null, new BigDecimal("8.00"), 10, kitchen));
        ProductService service = new ProductService(repository);

        assertThat(service.findAll())
                .extracting(Product::getName)
                .containsExactly("Mug", "Teapot");
    }

    @Test
    void findsNothingForAnUnknownId() {
        ProductService service = new ProductService(new InMemoryProductRepository());

        assertThat(service.findById(99L)).isEmpty();
    }

}
