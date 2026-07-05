package com.ecommerce.adminbackend.service;

import java.util.List;
import java.util.Map;

public interface ColorAdminService {

    List<Map<String, Object>> list();

    Map<String, Object> create(Map<String, Object> request);

    Map<String, Object> update(Long id, Map<String, Object> request);

    void delete(Long id);
}
