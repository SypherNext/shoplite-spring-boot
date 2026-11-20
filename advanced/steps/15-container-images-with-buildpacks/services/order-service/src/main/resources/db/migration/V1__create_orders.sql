-- Customers, orders and order lines: the order service's own database.

create table customers (
    id         bigint generated always as identity primary key,
    -- The "sub" claim of the customer's access token: stable even if the email changes.
    subject    varchar(100) not null unique,
    email      varchar(254) not null,
    full_name  varchar(200) not null,
    created_at timestamp with time zone not null
);

create table orders (
    id          bigint generated always as identity primary key,
    customer_id bigint         not null references customers (id),
    status      varchar(20)    not null,
    total       numeric(12, 2) not null,
    currency    varchar(3)     not null,
    payment_id  varchar(100),
    notify_url  varchar(500),
    created_at  timestamp with time zone not null,
    version     bigint         not null default 0
);

create index idx_orders_customer_id on orders (customer_id);
create index idx_orders_status on orders (status);

create table order_lines (
    id           bigint generated always as identity primary key,
    order_id     bigint         not null references orders (id) on delete cascade,
    -- The product lives in the catalog service's database, so there is no foreign key here:
    -- only its id and a copy of what the order needs.
    product_id   bigint         not null,
    product_sku  varchar(40)    not null,
    product_name varchar(120)   not null,
    unit_price   numeric(10, 2) not null,
    quantity     integer        not null check (quantity > 0)
);

create index idx_order_lines_order_id on order_lines (order_id);
