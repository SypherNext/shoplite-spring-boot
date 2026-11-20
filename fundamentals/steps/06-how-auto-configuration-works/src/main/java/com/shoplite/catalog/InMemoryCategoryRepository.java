package com.shoplite.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps categories in a map. Everything is lost when the app stops.
 * A plain class: InMemoryCatalogConfig creates it and decides what goes in it.
 */
public class InMemoryCategoryRepository implements CategoryRepository {

    // Concurrent types, because many HTTP requests can use this one object at the same time.
    private final Map<Long, Category> categories = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Category> findAll() {
        return List.copyOf(categories.values());
    }

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            category.setId(nextId.getAndIncrement());
        }
        categories.put(category.getId(), category);
        return category;
    }

}
