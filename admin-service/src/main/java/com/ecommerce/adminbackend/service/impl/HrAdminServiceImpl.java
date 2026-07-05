package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.AdminDepartment;
import com.ecommerce.adminbackend.entity.AdminJobApplication;
import com.ecommerce.adminbackend.entity.AdminJobOpening;
import com.ecommerce.adminbackend.repository.AdminDepartmentRepository;
import com.ecommerce.adminbackend.repository.AdminJobApplicationRepository;
import com.ecommerce.adminbackend.repository.AdminJobOpeningRepository;
import com.ecommerce.adminbackend.service.HrAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrAdminServiceImpl extends BaseAdminService implements HrAdminService {

    private final AdminDepartmentRepository departmentRepository;
    private final AdminJobOpeningRepository jobOpeningRepository;
    private final AdminJobApplicationRepository jobApplicationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDepartment)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createDepartment(AdminDepartment input) {
        requireNonBlank(input.getName(), "Department name");
        return toDepartment(departmentRepository.save(input));
    }

    @Override
    @Transactional
    public Map<String, Object> updateDepartment(Long id, AdminDepartment input) {
        AdminDepartment department = requireDepartment(id);
        if (input.getName() != null) {
            department.setName(input.getName());
        }
        if (input.getDescription() != null) {
            department.setDescription(input.getDescription());
        }
        if (input.getIcon() != null) {
            department.setIcon(input.getIcon());
        }
        if (input.getColor() != null) {
            department.setColor(input.getColor());
        }
        if (input.getActive() != null) {
            department.setActive(input.getActive());
        }
        return toDepartment(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        AdminDepartment department = requireDepartment(id);
        long jobCount = jobOpeningRepository.countByDepartmentId(department.getId());
        if (jobCount > 0) {
            throw new IllegalArgumentException("Cannot delete department with active job openings.");
        }
        departmentRepository.delete(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listJobs() {
        return jobOpeningRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toJob)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createJob(AdminJobOpening input) {
        requireNonBlank(input.getTitle(), "Job title");
        if (input.getStatus() != null) {
            input.setStatus(normalizeJobStatus(input.getStatus()));
        }
        if (input.getVacancies() == null || input.getVacancies() < 1) {
            input.setVacancies(1);
        }
        return toJob(jobOpeningRepository.save(input));
    }

    @Override
    @Transactional
    public Map<String, Object> updateJob(Long id, AdminJobOpening input) {
        AdminJobOpening job = requireJob(id);
        if (input.getDepartmentId() != null) {
            job.setDepartmentId(input.getDepartmentId());
        }
        if (input.getTitle() != null) {
            job.setTitle(input.getTitle());
        }
        if (input.getDescription() != null) {
            job.setDescription(input.getDescription());
        }
        if (input.getLocation() != null) {
            job.setLocation(input.getLocation());
        }
        if (input.getEmploymentType() != null) {
            job.setEmploymentType(input.getEmploymentType());
        }
        if (input.getRequirements() != null) {
            job.setRequirements(input.getRequirements());
        }
        if (input.getExperienceRequired() != null) {
            job.setExperienceRequired(input.getExperienceRequired());
        }
        if (input.getSalaryRange() != null) {
            job.setSalaryRange(input.getSalaryRange());
        }
        if (input.getVacancies() != null) {
            job.setVacancies(input.getVacancies());
        }
        if (input.getStatus() != null) {
            job.setStatus(normalizeJobStatus(input.getStatus()));
        }
        return toJob(jobOpeningRepository.save(job));
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        requireJob(id);
        jobOpeningRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listApplications(Long jobId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = jobId != null
                ? jobApplicationRepository.findByJobIdOrderByAppliedAtDesc(jobId, pageable)
                : jobApplicationRepository.findAllByOrderByAppliedAtDesc(pageable);
        return PageResponse.from(result.map(this::toApplication));
    }

    @Override
    @Transactional
    public Map<String, Object> updateApplicationStatus(Long id, String status) {
        AdminJobApplication application = requireFound(
                jobApplicationRepository.findById(id),
                "Job application not found.");
        application.setStatus(requireNonBlank(status, "Status"));
        return toApplication(jobApplicationRepository.save(application));
    }

    private AdminDepartment requireDepartment(Long id) {
        return requireFound(departmentRepository.findById(id), "Department not found.");
    }

    private AdminJobOpening requireJob(Long id) {
        return requireFound(jobOpeningRepository.findById(id), "Job opening not found.");
    }

    private Map<String, Object> toDepartment(AdminDepartment department) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", department.getId());
        row.put("name", department.getName());
        row.put("description", department.getDescription());
        row.put("icon", department.getIcon());
        row.put("color", department.getColor());
        row.put("active", department.getActive());
        row.put("jobCount", jobOpeningRepository.countByDepartmentId(department.getId()));
        row.put("createdAt", department.getCreatedAt());
        return row;
    }

    private String normalizeJobStatus(String status) {
        if (status == null || status.isBlank()) {
            return "open";
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active", "open" -> "open";
            case "paused", "inactive" -> "paused";
            case "closed" -> "closed";
            default -> normalized;
        };
    }

    private Map<String, Object> toJob(AdminJobOpening job) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", job.getId());
        row.put("departmentId", job.getDepartmentId());
        row.put("title", job.getTitle());
        row.put("description", job.getDescription());
        row.put("location", job.getLocation());
        row.put("employmentType", job.getEmploymentType());
        row.put("requirements", job.getRequirements());
        row.put("experienceRequired", job.getExperienceRequired());
        row.put("salaryRange", job.getSalaryRange());
        row.put("vacancies", job.getVacancies());
        row.put("status", job.getStatus());
        row.put("createdAt", job.getCreatedAt());
        row.put("applicationCount", jobApplicationRepository.countByJobId(job.getId()));
        return row;
    }

    private Map<String, Object> toApplication(AdminJobApplication application) {
        AdminJobOpening job = application.getJobId() != null
                ? jobOpeningRepository.findById(application.getJobId()).orElse(null)
                : null;
        String jobTitle = job != null ? job.getTitle() : null;
        String departmentName = null;
        if (job != null && job.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(job.getDepartmentId())
                    .map(AdminDepartment::getName)
                    .orElse(null);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", application.getId());
        row.put("jobId", application.getJobId());
        row.put("jobTitle", jobTitle);
        row.put("departmentName", departmentName);
        row.put("name", application.getName());
        row.put("email", application.getEmail());
        row.put("phone", application.getPhone());
        row.put("resumePath", application.getResumePath());
        row.put("coverLetter", application.getCoverLetter());
        row.put("status", application.getStatus());
        row.put("appliedAt", application.getAppliedAt());
        return row;
    }
}
