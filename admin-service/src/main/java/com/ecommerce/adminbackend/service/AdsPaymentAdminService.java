package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface AdsPaymentAdminService {

    PageResponse<Map<String, Object>> list(String search, String status, int page, int size);

    Map<String, Object> get(Integer id);

    Map<String, Object> stats();
}
