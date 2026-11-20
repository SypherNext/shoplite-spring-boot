-- The transactional outbox: messages waiting to be sent to Kafka.
-- A row is written in the SAME transaction as the order change it describes,
-- so either both are saved or neither is. The relay sends rows and stamps published_at.
create table outbox_events (
    id           uuid primary key,
    topic        varchar(200)             not null,
    message_key  varchar(200)             not null,
    event_type   varchar(50)              not null,
    payload      jsonb                    not null,
    created_at   timestamp with time zone not null,
    published_at timestamp with time zone
);

-- The relay only ever looks for unpublished rows, oldest first. A partial index keeps that
-- lookup small even when millions of published rows are still in the table.
create index idx_outbox_unpublished on outbox_events (created_at) where published_at is null;
