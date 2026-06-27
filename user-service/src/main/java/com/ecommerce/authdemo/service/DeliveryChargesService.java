package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.DeliveryChargeRequest;
import com.ecommerce.authdemo.dto.DeliveryChargeResponse;

import java.math.BigDecimal;
import java.util.List;

public interface DeliveryChargesService {
    DeliveryChargeResponse create(DeliveryChargeRequest request);

    List<DeliveryChargeResponse> getAll(Boolean status);

    DeliveryChargeResponse update(Integer id, DeliveryChargeRequest request);

    DeliveryChargeResponse updateStatus(Integer id, Boolean status);

    DeliveryChargeResponse getByWeight(BigDecimal weight);
}
