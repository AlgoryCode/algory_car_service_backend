-- Optional link from a work order line to the part of the vehicle it was done on, for the part history diagram.
ALTER TABLE work_order_items ADD COLUMN part VARCHAR(30);
ALTER TABLE work_order_items ADD COLUMN operation VARCHAR(15);
