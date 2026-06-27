package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.SizeChartRequest;
import com.ecommerce.sellerbackend.dto.SizeChartResponse;

import java.util.List;

public interface SizeChartService {

    List<SizeChartResponse> listForSeller(Long sellerId);

    SizeChartResponse getForSeller(Long sellerId, Integer id);

    SizeChartResponse create(Long sellerId, SizeChartRequest request);

    SizeChartResponse update(Long sellerId, Integer id, SizeChartRequest request);
}
