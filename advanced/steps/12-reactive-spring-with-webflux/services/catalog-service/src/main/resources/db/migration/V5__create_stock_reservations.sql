-- Stock reservations made for the order service, one row per reservation id.
-- The order service picks the id, so a retried call (after a timeout, say) finds the row
-- and gets the same answer instead of taking the stock a second time.
-- Releasing a reservation deletes its row, so the same id can reserve again afterwards.
create table stock_reservations (
    id          uuid primary key,
    lines       jsonb                    not null,
    products    jsonb                    not null,
    created_at  timestamp with time zone not null
);
