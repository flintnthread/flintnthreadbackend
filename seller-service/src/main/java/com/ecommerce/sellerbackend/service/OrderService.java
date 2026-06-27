package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.order.SellerOrderDetailDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderStatsDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderSummaryDto;

import java.util.List;

public interface OrderService {

    List<SellerOrderSummaryDto> listForSeller(Long sellerId);

    List<SellerOrderDetailDto> listDetailsForSeller(Long sellerId);

    SellerOrderStatsDto statsForSeller(Long sellerId);

    SellerOrderDetailDto getForSeller(Long sellerId, String orderKey);

    SellerOrderDetailDto updateStatusForSeller(Long sellerId, String orderKey, String status, String comment);
}
