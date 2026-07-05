-- Extend admin job openings with fields used by the admin careers UI

ALTER TABLE admin_job_openings
    ADD COLUMN requirements TEXT NULL,
    ADD COLUMN experience_required VARCHAR(100) NULL,
    ADD COLUMN salary_range VARCHAR(100) NULL,
    ADD COLUMN vacancies INT NOT NULL DEFAULT 1;
