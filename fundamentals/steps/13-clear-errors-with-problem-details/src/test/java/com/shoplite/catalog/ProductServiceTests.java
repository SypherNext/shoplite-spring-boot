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

    private final InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
    private final InMemoryProductRepository repository = new InMemoryProductRepository();
    private final ProductService service = new ProductService(repository,
            new CategoryService(categories, repository), settings);
    private final Category kitchen = categories.save(new Category("Kitchen", null));

    @Test
    void listsProductsSortedByName() {
        repository.save(new Product("Teapot", "TEAPOT-1", null, new BigDecimal("30.00"), 4, kitchen));
        repository.save(new Product("Mug", "MUG-1", null, new BigDecimal("8.00"), 10, kitchen));

        assertThat(service.findAll())
                .extracting(ProductResponse::name)
                .containsExactly("Mug", "Teapot");
    }

    @Test
    void throwsNotFoundForAnUnknownId() {
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product 99 was not found");
    }

    @Test
    void createsAProductInItsCategory() {
        ProductResponse created = service.create(new ProductRequest("Mug", "MUG-1", null,
                new BigDecimal("8.00"), 10, null, kitchen.getId()));

        assertThat(created.categoryName()).isEqualTo("Kitchen");
        assertThat(created.active()).isTrue();
    }

}
