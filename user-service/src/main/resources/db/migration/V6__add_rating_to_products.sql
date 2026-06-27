-- Add missing rating column to products table
ALTER TABLE products 
ADD COLUMN rating DECIMAL(3,2) DEFAULT 0.0;

-- Add rating_count column to products table
ALTER TABLE products 
ADD COLUMN rating_count INT DEFAULT 0;
