-- "Name contains ..." searches (lower(name) like '%mug%') cannot use a normal B-tree index: the
-- pattern starts with a wildcard. A trigram index can. pg_trgm ships with PostgreSQL and is a
-- trusted extension, so the database owner may create it.
create extension if not exists pg_trgm;

create index idx_products_name_trgm on products using gin (lower(name) gin_trgm_ops);
