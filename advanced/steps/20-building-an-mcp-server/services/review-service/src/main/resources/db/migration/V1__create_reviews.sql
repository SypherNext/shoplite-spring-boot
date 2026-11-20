create table reviews (
    id            bigint generated always as identity primary key,
    -- The product lives in the catalog service: an id only, no foreign key.
    product_id    bigint                   not null,
    -- The "sub" claim of the reviewer's token, and the name shown with the review.
    author        varchar(100)             not null,
    author_name   varchar(200)             not null,
    rating        smallint                 not null check (rating between 1 and 5),
    comment       varchar(2000),
    created_at    timestamp with time zone not null
);

-- One review per customer and product.
create unique index uq_reviews_product_author on reviews (product_id, author);
