package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.entity.AdminDepartment;
import com.ecommerce.adminbackend.entity.AdminJobOpening;
import com.ecommerce.adminbackend.service.HrAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class HrAdminController {

    private static final Logger log = LogFactory.getLogger(HrAdminController.class);

    private final HrAdminService hrAdminService;

    @GetMapping("/departments")
    public List<Map<String, Object>> listDepartments() {
        return hrAdminService.listDepartments();
    }

    @PostMapping("/departments")
    public Map<String, Object> createDepartment(@RequestBody AdminDepartment request) {
        return hrAdminService.createDepartment(request);
    }

    @PutMapping("/departments/{id}")
    public Map<String, Object> updateDepartment(@PathVariable Long id, @RequestBody AdminDepartment request) {
        return hrAdminService.updateDepartment(id, request);
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Map<String, String>> deleteDepartment(@PathVariable Long id) {
        hrAdminService.deleteDepartment(id);
        return ResponseEntity.ok(Map.of("message", "Department deleted."));
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> listJobs() {
        return hrAdminService.listJobs();
    }

    @PostMapping("/jobs")
    public Map<String, Object> createJob(@RequestBody AdminJobOpening request) {
        return hrAdminService.createJob(request);
    }

    @PutMapping("/jobs/{id}")
    public Map<String, Object> updateJob(@PathVariable Long id, @RequestBody AdminJobOpening request) {
        return hrAdminService.updateJob(id, request);
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long id) {
        hrAdminService.deleteJob(id);
        return ResponseEntity.ok(Map.of("message", "Job deleted."));
    }

    @GetMapping("/job-applications")
    public PageResponse<Map<String, Object>> listApplications(
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return hrAdminService.listApplications(jobId, page, size);
    }

    @PatchMapping("/job-applications/{id}")
    public Map<String, Object> updateApplication(@PathVariable Long id, @RequestBody NoteRequest request) {
        return hrAdminService.updateApplicationStatus(id, request != null ? request.getStatus() : null);
    }
}
