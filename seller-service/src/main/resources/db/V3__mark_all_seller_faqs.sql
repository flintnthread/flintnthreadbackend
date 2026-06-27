-- Seller app shows ONLY rows where is_seller = 1 AND status = 1.
-- All buyer FAQs stay is_seller = 0 (hidden from seller app).

-- Example: show one FAQ in the seller app
-- UPDATE shopping.faqs SET is_seller = 1 WHERE id = 38;

-- Example: show all FAQs in a category for sellers
-- UPDATE shopping.faqs SET is_seller = 1 WHERE category_id = 14 AND status = 1;

-- Hide a FAQ from seller app
-- UPDATE shopping.faqs SET is_seller = 0 WHERE id = 38;

-- Check which FAQs the seller app will load:
-- SELECT id, question, is_seller FROM shopping.faqs WHERE status = 1 AND is_seller = 1;
