CREATE TABLE IF NOT EXISTS sellers (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    seller_code     VARCHAR(30)     UNIQUE,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100),
    phone           VARCHAR(15)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash   VARCHAR(255)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'active',
    otp             VARCHAR(10),
    otp_expires_at  DATETIME,
    ip_address      VARCHAR(60),
    device_info     VARCHAR(255),
    last_login      DATETIME,
    remember_token  VARCHAR(255),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_sellers_email (email),
    INDEX idx_sellers_phone (phone),
    INDEX idx_sellers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
