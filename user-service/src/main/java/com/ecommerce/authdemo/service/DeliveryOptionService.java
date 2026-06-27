package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.DeliveryOptionRequest;
import com.ecommerce.authdemo.dto.DeliveryOptionResponse;

import java.util.List;

public interface DeliveryOptionService {
    DeliveryOptionResponse create(DeliveryOptionRequest request);

    List<DeliveryOptionResponse> getAll(Integer sellerId, Boolean isActive);

    DeliveryOptionResponse update(Integer id, DeliveryOptionRequest request);

    DeliveryOptionResponse updateStatus(Integer id, Boolean isActive);
}
