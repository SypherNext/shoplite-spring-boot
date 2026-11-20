package com.shoplite.catalog;

/** What the API returns for a category. The entity itself never leaves the service. */
public record CategoryResponse(Long id, String name, String description) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

}
