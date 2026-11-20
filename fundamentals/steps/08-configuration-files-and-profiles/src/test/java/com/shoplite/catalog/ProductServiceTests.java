package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductServiceTests {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final Category kitchen = new Category("Kitchen", null);
    private final InMemoryProductRepository repository = new InMemoryProductRepository();
    private final ProductService service = new ProductService(repository, LOW_STOCK_THRESHOLD);

    @Test
    void listsProductsSortedByName() {
        repository.save(new Product("Teapot", "TEAPOT-1", null, new BigDecimal("30.00"), 4, kitchen));
        repository.save(new Product("Mug", "MUG-1", null, new BigDecimal("8.00"), 10, kitchen));

        assertThat(service.findAll())
                .extracting(Product::getName)
                .containsExactly("Mug", "Teapot");
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void listsActiveProductsAtOrBelowTheThreshold() {
        repository.save(new Product("Teapot", "TEAPOT-1", null, new BigDecimal("30.00"), 5, kitchen));
        repository.save(new Product("Mug", "MUG-1", null, new BigDecimal("8.00"), 6, kitchen));
        Product retired = new Product("Old Jug", "JUG-OLD", null, new BigDecimal("15.00"), 0, kitchen);
        retired.setActive(false);
        repository.save(retired);

        assertThat(service.findLowStock())
                .extracting(Product::getSku)
                .containsExactly("TEAPOT-1");
    }

}
