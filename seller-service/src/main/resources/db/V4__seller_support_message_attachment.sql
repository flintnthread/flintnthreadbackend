-- Support ticket message image attachments (run once if column missing)
ALTER TABLE seller_support_messages
    ADD COLUMN attachment VARCHAR(512) NULL AFTER message;
