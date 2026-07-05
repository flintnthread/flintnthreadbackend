package com.ecommerce.adminbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSchemaPatchRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureHrTables();
        addColumnIfMissing("admin_job_openings", "requirements", "TEXT NULL");
        addColumnIfMissing("admin_job_openings", "experience_required", "VARCHAR(100) NULL");
        addColumnIfMissing("admin_job_openings", "salary_range", "VARCHAR(100) NULL");
        addColumnIfMissing("admin_job_openings", "vacancies", "INT NOT NULL DEFAULT 1");
    }

    private void ensureHrTables() {
        if (!tableExists("admin_departments")) {
            jdbcTemplate.execute("""
                    CREATE TABLE admin_departments (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        description TEXT NULL,
                        icon VARCHAR(50) NULL,
                        color VARCHAR(30) NULL,
                        active TINYINT(1) NOT NULL DEFAULT 1,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            log.info("Created table admin_departments");
        }

        if (!tableExists("admin_job_openings")) {
            jdbcTemplate.execute("""
                    CREATE TABLE admin_job_openings (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        department_id BIGINT NULL,
                        title VARCHAR(200) NOT NULL,
                        description TEXT NULL,
                        location VARCHAR(150) NULL,
                        employment_type VARCHAR(50) NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'open',
                        requirements TEXT NULL,
                        experience_required VARCHAR(100) NULL,
                        salary_range VARCHAR(100) NULL,
                        vacancies INT NOT NULL DEFAULT 1,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
                        KEY idx_job_department (department_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            log.info("Created table admin_job_openings");
        }

        if (!tableExists("admin_job_applications")) {
            jdbcTemplate.execute("""
                    CREATE TABLE admin_job_applications (
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            log.info("Created table admin_job_applications");
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                        """,
                Integer.class,
                table);
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        if (!tableExists(table)) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                table,
                column);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        log.info("Added missing column {}.{}", table, column);
    }
}
