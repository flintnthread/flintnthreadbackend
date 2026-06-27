package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.AdminDepartment;
import com.ecommerce.adminbackend.entity.AdminJobOpening;

import java.util.List;
import java.util.Map;

public interface HrAdminService {

    List<Map<String, Object>> listDepartments();

    Map<String, Object> createDepartment(AdminDepartment input);

    Map<String, Object> updateDepartment(Long id, AdminDepartment input);

    void deleteDepartment(Long id);

    List<Map<String, Object>> listJobs();

    Map<String, Object> createJob(AdminJobOpening input);

    Map<String, Object> updateJob(Long id, AdminJobOpening input);

    void deleteJob(Long id);

    PageResponse<Map<String, Object>> listApplications(Long jobId, int page, int size);

    Map<String, Object> updateApplicationStatus(Long id, String status);
}
