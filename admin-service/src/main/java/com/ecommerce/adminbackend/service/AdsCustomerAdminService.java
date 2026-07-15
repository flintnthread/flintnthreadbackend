package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface AdsCustomerAdminService {

    PageResponse<Map<String, Object>> list(String search, int page, int size);

    Map<String, Object> get(Integer id);

    Map<String, Object> delete(Integer id);
}
