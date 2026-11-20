-- Spring Modulith's Event Publication Registry (schema v2, copied from
-- spring-modulith-events-jdbc 2.1.2: org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql).
-- One row per (event, listener): written when the event is published inside a transaction,
-- marked completed when that listener finishes. Rows that never complete can be resubmitted.
create table event_publication (
    id                     uuid                     not null,
    listener_id            text                     not null,
    event_type             text                     not null,
    serialized_event       text                     not null,
    publication_date       timestamp with time zone not null,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamp with time zone,
    primary key (id)
);

create index event_publication_serialized_event_hash_idx on event_publication using hash (serialized_event);
create index event_publication_by_completion_date_idx on event_publication (completion_date);
