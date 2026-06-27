-- Remove product-level customizable flag; cart/order lines keep customization_image_url.
ALTER TABLE products
    DROP COLUMN is_customizable;
