package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to existing {@code career_jobs} (legacy careers schema).
 */
@Entity
@Table(name = "career_jobs")
@Getter
@Setter
public class AdminJobOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(length = 255)
    private String location;

    /** DB enum: full-time | part-time | contract | internship */
    @Column(name = "employment_type", length = 50)
    private String employmentType = "full-time";

    @Column(name = "experience_required", length = 100)
    private String experienceRequired;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    private Integer vacancies = 1;

    /** DB enum: active | inactive | closed */
    @Column(nullable = false, length = 30)
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (description == null) {
            description = "";
        }
        if (vacancies == null || vacancies < 1) {
            vacancies = 1;
        }
        if (employmentType == null || employmentType.isBlank()) {
            employmentType = "full-time";
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
