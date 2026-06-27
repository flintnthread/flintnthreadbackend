-- Ensure seller app always receives FAQs by marking all active FAQs as seller-visible.
-- This prevents an "empty FAQ list" when the database wasn't pre-seeded with is_seller flags.
UPDATE faqs
SET is_seller = 1
WHERE status = 1;

