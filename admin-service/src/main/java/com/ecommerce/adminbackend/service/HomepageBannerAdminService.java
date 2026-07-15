package com.ecommerce.adminbackend.service;

import java.util.List;
import java.util.Map;

public interface HomepageBannerAdminService {

    List<Map<String, Object>> list(String section);

    Map<String, Object> get(Integer id);

    Map<String, Object> create(Map<String, Object> body);

    Map<String, Object> update(Integer id, Map<String, Object> body);

    void delete(Integer id);
}
