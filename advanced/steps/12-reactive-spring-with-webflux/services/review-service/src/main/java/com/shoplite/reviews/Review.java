package com.shoplite.reviews;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row of the reviews table. Spring Data R2DBC maps records directly: no JPA, no lazy loading,
 * no persistence context. A null id means "not saved yet".
 */
@Table("reviews")
public record Review(@Id Long id, Long productId, String author, String authorName, int rating, String comment,
        Instant createdAt) {
}
