-- Repoint wallet_transactions FKs for FNT user wallet (buyer users.id).

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
