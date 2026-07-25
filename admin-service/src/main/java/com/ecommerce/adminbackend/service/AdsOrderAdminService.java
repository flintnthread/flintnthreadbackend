package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface AdsOrderAdminService {

    PageResponse<Map<String, Object>> list(String search, String status, String billingType, Integer userId, int page, int size);

    Map<String, Object> get(Integer id);

    Map<String, Object> stats();
}
