-- Remembers which order each Idempotency-Key created, so a retried request
-- returns the same order instead of creating a second one.
create table idempotency_keys (
    id               bigint generated always as identity primary key,
    customer_id      bigint      not null references customers (id),
    idempotency_key  varchar(100) not null,
    -- SHA-256 of the request body: the same key with a different body is refused.
    request_hash     varchar(64) not null,
    order_id         bigint      not null references orders (id),
    created_at       timestamp with time zone not null,
    -- The database, not the Java code, has the final word when two copies race.
    constraint uq_idempotency_customer_key unique (customer_id, idempotency_key)
);

create index idx_idempotency_keys_created_at on idempotency_keys (created_at);
