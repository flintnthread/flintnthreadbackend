-- Add variant_id column to user_wishlist table
ALTER TABLE user_wishlist 
ADD COLUMN variant_id BIGINT;

-- Add index for better performance on queries with variant_id
CREATE INDEX idx_user_wishlist_variant ON user_wishlist(user_id, product_id, variant_id);
