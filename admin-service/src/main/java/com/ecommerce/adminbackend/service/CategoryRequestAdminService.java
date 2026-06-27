package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface CategoryRequestAdminService {

    PageResponse<Map<String, Object>> listRequests(String status, int page, int size);

    Map<String, Object> stats();

    Map<String, Object> approve(Long id, String note);

    Map<String, Object> reject(Long id, String note);
}
