-- Run this SQL manually on your database to add the mrp_price column
-- Required for showing MRP/strikethrough price in orders list
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS mrp_price DOUBLE DEFAULT NULL;

-- Ensure other order_item columns exist (they should already exist, but just in case)
-- ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(500) DEFAULT NULL;
-- ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_image_path VARCHAR(1000) DEFAULT NULL;
-- ALTER TABLE order_items ADD COLUMN IF NOT EXISTS color VARCHAR(100) DEFAULT NULL;
-- ALTER TABLE order_items ADD COLUMN IF NOT EXISTS size VARCHAR(100) DEFAULT NULL;
-- ALTER TABLE order_items ADD COLUMN IF NOT EXISTS sku VARCHAR(200) DEFAULT NULL;
-- ALTER TABLE order_items ADD COLUMN IF NOT EXISTS weight DOUBLE DEFAULT NULL;
