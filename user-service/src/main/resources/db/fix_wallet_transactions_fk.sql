-- Fix FNT wallet foreign keys on wallet_transactions.
-- seller_id and created_by store buyer users.id, not sellers/admin_users.

ALTER TABLE wallet_transactions
    DROP FOREIGN KEY wallet_transactions_ibfk_1;

ALTER TABLE wallet_transactions
    ADD CONSTRAINT fk_wallet_transactions_user
        FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE wallet_transactions
    DROP FOREIGN KEY wallet_transactions_ibfk_3;

ALTER TABLE wallet_transactions
    ADD CONSTRAINT fk_wallet_transactions_created_by_user
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;
