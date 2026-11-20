-- Catalog tables. Flyway runs each V<number>__<name>.sql file once, in order,
-- and records it in the flyway_schema_history table.

create table categories (
    id          bigint generated always as identity primary key,
    name        varchar(80)  not null unique,
    description varchar(500)
);

create table products (
    id          bigint generated always as identity primary key,
    name        varchar(120)   not null,
    sku         varchar(40)    not null unique,
    description varchar(2000),
    price       numeric(10, 2) not null check (price >= 0),
    stock       integer        not null check (stock >= 0),
    active      boolean        not null default true,
    category_id bigint         not null references categories (id)
);

-- Foreign keys are not indexed automatically in PostgreSQL.
create index idx_products_category_id on products (category_id);
