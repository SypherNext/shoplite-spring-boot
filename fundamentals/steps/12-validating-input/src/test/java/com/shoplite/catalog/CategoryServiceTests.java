package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** A plain unit test: no Spring, you pass the dependency in yourself. */
class CategoryServiceTests {

    private final InMemoryCategoryRepository repository = new InMemoryCategoryRepository();
    private final CategoryService service = new CategoryService(repository);

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

}
