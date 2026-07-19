-- Custom label for "other" address type (e.g. "Mom's House", "Guest Flat").
-- Apply manually if ddl-auto=none (MySQL). Skip if the column already exists.

ALTER TABLE addresses
    ADD COLUMN label VARCHAR(100) NULL;
