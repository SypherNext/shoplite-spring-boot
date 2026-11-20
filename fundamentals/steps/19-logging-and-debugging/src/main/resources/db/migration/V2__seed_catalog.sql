-- Starter data so the API has something to show on the first run.

insert into categories (name, description) values
    ('Kitchen', 'Mugs, bottles and things for your coffee break'),
    ('Desk', 'Notebooks, pens and desk accessories'),
    ('Apparel', 'T-shirts and hoodies');

insert into products (name, sku, description, price, stock, active, category_id) values
    ('Enamel Mug', 'MUG-ENAMEL-01', 'Speckled enamel camping mug, 350 ml', 12.50, 40, true,
        (select id from categories where name = 'Kitchen')),
    ('Steel Water Bottle', 'BOTTLE-STEEL-750', 'Insulated bottle, keeps drinks cold for 24 hours', 24.90, 3, true,
        (select id from categories where name = 'Kitchen')),
    ('Pour-Over Coffee Set', 'COFFEE-POUR-SET', 'Glass dripper with 100 paper filters', 34.00, 12, true,
        (select id from categories where name = 'Kitchen')),
    ('Dotted Notebook A5', 'NOTE-A5-DOT', '160 pages of 100 gsm dotted paper', 9.80, 75, true,
        (select id from categories where name = 'Desk')),
    ('Gel Pen Pack', 'PEN-GEL-5PK', 'Five black gel pens, 0.5 mm', 6.40, 2, true,
        (select id from categories where name = 'Desk')),
    ('Monitor Stand', 'DESK-STAND-OAK', 'Solid oak monitor riser', 49.00, 0, false,
        (select id from categories where name = 'Desk')),
    ('Logo T-Shirt', 'TEE-LOGO-M', 'Organic cotton T-shirt, size M', 19.00, 25, true,
        (select id from categories where name = 'Apparel')),
    ('Zip Hoodie', 'HOODIE-ZIP-L', 'Heavyweight zip hoodie, size L', 54.00, 5, true,
        (select id from categories where name = 'Apparel'));
