-- Profile fields used by mobile settings / account (apply if ddl-auto=none).
-- Skip any statement if the column already exists.

ALTER TABLE users
    ADD COLUMN date_of_birth DATE NULL;

ALTER TABLE users
    ADD COLUMN gender VARCHAR(32) NULL;
