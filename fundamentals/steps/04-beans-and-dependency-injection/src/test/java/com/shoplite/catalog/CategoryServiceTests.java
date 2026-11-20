package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** A plain unit test: no Spring, you pass the dependency in yourself. */
class CategoryServiceTests {

    @Test
    void listsCategoriesSortedByName() {
        CategoryService service = new CategoryService(new InMemoryCategoryRepository());

        assertThat(service.findAll())
                .extracting(Category::getName)
                .containsExactly("Apparel", "Desk", "Kitchen");
    }

    @Test
    void includesCategoriesAddedLater() {
        InMemoryCategoryRepository repository = new InMemoryCategoryRepository();
        repository.save(new Category("Books", "Paper you can read"));
        CategoryService service = new CategoryService(repository);

        assertThat(service.findAll())
                .extracting(Category::getName)
                .containsExactly("Apparel", "Books", "Desk", "Kitchen");
    }

}
