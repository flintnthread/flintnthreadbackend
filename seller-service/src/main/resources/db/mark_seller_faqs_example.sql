-- Mark FAQs as seller-only (is_seller = 1) so they show in the seller app.
-- Buyer FAQs stay is_seller = 0 and are hidden from the seller app.

-- All active FAQs (applied in V3__mark_all_seller_faqs.sql):
UPDATE shopping.faqs SET is_seller = 1 WHERE status = 1;

-- Or mark only specific rows:
-- UPDATE shopping.faqs SET is_seller = 1 WHERE id IN (32, 33, 37, 38, 41);

-- Or mark all FAQs in a category:
-- UPDATE shopping.faqs SET is_seller = 1 WHERE category_id = 18 AND status = 1;
