-- Admin platform tables (new tables only)

CREATE TABLE IF NOT EXISTS admin_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    icon VARCHAR(50) NULL,
    color VARCHAR(30) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_job_openings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    location VARCHAR(150) NULL,
    employment_type VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'open',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_job_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_job_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    resume_path VARCHAR(500) NULL,
    cover_letter TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_job_app_job (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS delivery_weight_slabs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    min_weight_kg DECIMAL(10,3) NOT NULL,
    max_weight_kg DECIMAL(10,3) NOT NULL,
    intra_city_charge DECIMAL(10,2) NOT NULL,
    metro_metro_charge DECIMAL(10,2) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'commission_b2c', '15'
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'commission_b2c');

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'commission_b2b', '7'
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'commission_b2b');

-- Contact reply storage (safe if column already exists)
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND COLUMN_NAME = 'admin_notes'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE contacts ADD COLUMN admin_notes TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
