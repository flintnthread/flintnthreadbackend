package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface AdsNotificationAdminService {

    PageResponse<Map<String, Object>> list(String search, String status, Boolean unreadOnly, int page, int size);

    Map<String, Object> patch(Integer id, Map<String, Object> body);

    Map<String, Object> stats();
}
