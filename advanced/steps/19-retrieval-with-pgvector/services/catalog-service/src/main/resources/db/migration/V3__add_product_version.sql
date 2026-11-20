-- Optimistic locking for products: every update bumps the version and checks the old one.
alter table products add column version bigint not null default 0;
