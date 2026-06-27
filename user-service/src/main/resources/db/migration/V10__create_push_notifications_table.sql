CREATE TABLE IF NOT EXISTS push_notifications (
    id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'general',
    link VARCHAR(500),
    is_read TINYINT(1) DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_push_notifications_user_id (user_id),
    KEY idx_push_notifications_is_read (is_read),
    KEY idx_push_notifications_created_at (created_at),
    CONSTRAINT fk_push_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
);
