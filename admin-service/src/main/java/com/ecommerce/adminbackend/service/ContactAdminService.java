package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface ContactAdminService {

    PageResponse<Map<String, Object>> listContacts(int page, int size);

    Map<String, Object> stats();

    Map<String, Object> getContact(Integer id);

    Map<String, Object> updateStatus(Integer id, Boolean status);

    Map<String, Object> reply(Integer id, String reply);

    void deleteContact(Integer id);
}
