package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.shoplite.common.ConflictException;

/** A plain unit test: no Spring, you pass the dependencies in yourself. */
class CategoryServiceTests {

    private final InMemoryCategoryRepository repository = new InMemoryCategoryRepository();
    private final CategoryService service = new CategoryService(repository, new InMemoryProductRepository());

    @Test
    void listsCategoriesSortedByName() {
        repository.save(new Category("Kitchen", null));
        repository.save(new Category("Books", null));

        assertThat(service.findAll())
                .extracting(CategoryResponse::name)
                .containsExactly("Books", "Kitchen");
    }

    @Test
    void createsACategoryWithANewId() {
        CategoryResponse created = service.create(new CategoryRequest("Garden", "Pots and seeds"));

        assertThat(created.id()).isNotNull();
        assertThat(service.findById(created.id()).name()).isEqualTo("Garden");
    }

    @Test
    void refusesASecondCategoryWithTheSameName() {
        service.create(new CategoryRequest("Garden", null));

        assertThatThrownBy(() -> service.create(new CategoryRequest("GARDEN", null)))
                .isInstanceOf(ConflictException.class);
    }

}
