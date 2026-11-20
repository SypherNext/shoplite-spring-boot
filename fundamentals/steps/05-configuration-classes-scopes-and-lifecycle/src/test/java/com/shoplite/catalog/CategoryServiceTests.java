package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** A plain unit test: no Spring, you pass the dependency in yourself. */
class CategoryServiceTests {

    @Test
    void listsCategoriesSortedByName() {
        InMemoryCategoryRepository repository = new InMemoryCategoryRepository();
        repository.save(new Category("Kitchen", null));
        repository.save(new Category("Books", null));
        CategoryService service = new CategoryService(repository);

        assertThat(service.findAll())
                .extracting(Category::getName)
                .containsExactly("Books", "Kitchen");
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoCategories() {
        CategoryService service = new CategoryService(new InMemoryCategoryRepository());

        assertThat(service.findAll()).isEmpty();
    }

}
