package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.shoplite.common.NotFoundException;
import com.shoplite.shop.ShopProperties;

class ProductServiceTests {

    // A record is easy to build in a test: no Spring, no properties file.
    private final ShopProperties settings = new ShopProperties("Test Shop", "EUR", 5);

    private final Category kitchen = new Category("Kitchen", null);
    private final InMemoryProductRepository repository = new InMemoryProductRepository();
    private final ProductService service = new ProductService(repository, settings);

    @Test
    void listsProductsSortedByName() {
        repository.save(new Product("Teapot", "TEAPOT-1", null, new BigDecimal("30.00"), 4, kitchen));
        repository.save(new Product("Mug", "MUG-1", null, new BigDecimal("8.00"), 10, kitchen));

        assertThat(service.findAll())
                .extracting(Product::getName)
                .containsExactly("Mug", "Teapot");
    }

    @Test
    void throwsNotFoundForAnUnknownId() {
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product 99 was not found");
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
