package com.shoplite.catalog;

/** What a client sends to create or update a category. */
public record CategoryRequest(String name, String description) {
}
