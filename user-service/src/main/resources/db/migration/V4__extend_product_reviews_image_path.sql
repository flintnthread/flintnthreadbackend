-- Cloudinary secure URLs commonly exceed VARCHAR(255); widen to avoid review insert failures.
ALTER TABLE product_reviews
    MODIFY COLUMN image_path VARCHAR(2048) NULL;
