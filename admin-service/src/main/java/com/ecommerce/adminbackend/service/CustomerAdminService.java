package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface CustomerAdminService {

    PageResponse<Map<String, Object>> listCustomers(String search, int page, int size);

    Map<String, Object> stats();

    Map<String, Object> getCustomer(Long id);

    Map<String, Object> getCustomerAnalytics(Long id);
}
