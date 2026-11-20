package com.shoplite.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/** Keeps categories in a map. Everything is lost when the app stops. */
@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    // Concurrent types, because many HTTP requests can use this one object at the same time.
    private final Map<Long, Category> categories = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public InMemoryCategoryRepository() {
        save(new Category("Kitchen", "Mugs, bottles and things for your coffee break"));
        save(new Category("Desk", "Notebooks, pens and desk accessories"));
        save(new Category("Apparel", "T-shirts and hoodies"));
    }

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
