package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.AdminJobOpening;
import com.ecommerce.adminbackend.service.HrAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class HrAdminIT {

    @Autowired
    private HrAdminService hrAdminService;

    @Test
    void listDepartmentsAndCreateJobWithExtendedFields() {
        List<Map<String, Object>> departments = hrAdminService.listDepartments();
        assertNotNull(departments);

        AdminJobOpening job = new AdminJobOpening();
        job.setTitle("QA Role " + UUID.randomUUID().toString().substring(0, 6));
        job.setDescription("Automated test job");
        job.setLocation("Remote");
        job.setEmploymentType("Full Time");
        job.setStatus("open");
        job.setRequirements("Java, Spring Boot");
        job.setExperienceRequired("2+ years");
        job.setSalaryRange("8-12 LPA");
        job.setVacancies(2);
        if (!departments.isEmpty()) {
            job.setDepartmentId(((Number) departments.get(0).get("id")).longValue());
        }

        Map<String, Object> created = hrAdminService.createJob(job);
        assertNotNull(created.get("id"));
        assertEquals(job.getRequirements(), created.get("requirements"));
        assertEquals(job.getExperienceRequired(), created.get("experienceRequired"));
        assertEquals(job.getSalaryRange(), created.get("salaryRange"));
        assertEquals(2, ((Number) created.get("vacancies")).intValue());

        Long jobId = ((Number) created.get("id")).longValue();
        List<Map<String, Object>> jobs = hrAdminService.listJobs();
        assertFalse(jobs.stream().noneMatch(row -> jobId.equals(((Number) row.get("id")).longValue())));

        hrAdminService.deleteJob(jobId);
    }
}
