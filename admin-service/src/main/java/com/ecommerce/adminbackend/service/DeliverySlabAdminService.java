package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.DeliveryCharge;

import java.util.List;
import java.util.Map;

public interface DeliverySlabAdminService {

    List<Map<String, Object>> listSlabs();

    Map<String, Object> create(DeliveryCharge input);

    Map<String, Object> update(Integer id, DeliveryCharge input);

    void delete(Integer id);
}
