-- Customer design image on cart/order lines (no product-level flag column).
ALTER TABLE cart
    ADD COLUMN customization_image_url VARCHAR(500) NULL;

ALTER TABLE order_items
    ADD COLUMN customization_image_url VARCHAR(500) NULL;
