package com.ecommerce.sellerbackend.service;

import java.util.Map;

public interface SellerAccountLifecycleService {

    Map<String, Object> getEligibility(Long sellerId);

    Map<String, Object> getStatus(Long sellerId);

    Map<String, Object> requestDeactivation(Long sellerId, String duration);

    Map<String, Object> requestActivation(Long sellerId);
}
