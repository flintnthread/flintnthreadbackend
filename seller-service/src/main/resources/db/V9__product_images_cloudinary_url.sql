-- Store Cloudinary HTTPS URLs in product_images.image_path
ALTER TABLE product_images
    MODIFY COLUMN image_path VARCHAR(1024) NOT NULL;
