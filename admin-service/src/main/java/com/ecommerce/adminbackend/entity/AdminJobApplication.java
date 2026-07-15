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
 * Maps to existing {@code career_applications} (legacy careers schema).
 */
@Entity
@Table(name = "career_applications")
@Getter
@Setter
public class AdminJobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "current_location", length = 255)
    private String currentLocation;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "current_company", length = 255)
    private String currentCompany;

    @Column(name = "current_designation", length = 255)
    private String currentDesignation;

    @Column(name = "expected_salary", length = 100)
    private String expectedSalary;

    @Column(name = "notice_period", length = 100)
    private String noticePeriod;

    @Column(name = "resume_file", length = 255)
    private String resumePath;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    /**
     * DB enum: pending | reviewed | shortlisted | rejected | interviewed | hired
     */
    @Column(nullable = false, length = 30)
    private String status = "pending";

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (appliedAt == null) {
            appliedAt = now;
        }
        if (status == null || status.isBlank()) {
            status = "pending";
        }
        if (phone == null) {
            phone = "";
        }
    }

    @PreUpdate
    void onUpdate() {
        // career_applications has no updated_at column
    }
}
