-- 50,000 extra products for load tests (development databases only).
-- Run against the catalog database:
--   docker exec -i shoplite-advanced-postgres-1 psql -U shoplite -d catalog < load-tests/seed-products.sql
insert into products (name, sku, description, price, stock, active, category_id, version)
select 'Load test ' || (array['mug', 'bottle', 'notebook', 'pen', 'hoodie', 'lamp', 'chair', 'desk'])[1 + g % 8]
           || ' ' || g,
       'LOAD-' || g,
       'Generated for load tests',
       1 + g % 100,
       1000,
       true,
       1 + g % 3,
       0
from generate_series(1, 50000) as g;

analyze products;
