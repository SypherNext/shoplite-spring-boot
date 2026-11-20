-- The catalog's memory of which order events it has already handled.
-- Kafka delivers at least once, so the same event can arrive twice. The primary key makes
-- "have I seen this event?" and "remember it" one atomic insert.
create table processed_events (
    event_id     uuid primary key,
    processed_at timestamp with time zone not null
);
