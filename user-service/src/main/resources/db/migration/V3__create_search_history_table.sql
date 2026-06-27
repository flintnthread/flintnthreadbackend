CREATE TABLE IF NOT EXISTS search_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    searched_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    session_id VARCHAR(100),
    user_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_search_history_user_id (user_id),
    KEY idx_search_history_session_id (session_id),
    KEY idx_search_history_searched_at (searched_at),
    CONSTRAINT fk_search_history_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL
);
