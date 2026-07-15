package com.ecommerce.adminbackend.service;

import java.util.List;
import java.util.Map;

public interface HomepageSectionAdminService {

    List<Map<String, Object>> list();

    List<Map<String, Object>> upsert(List<Map<String, Object>> items);
}
