package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.entity.Order;

public interface ShiprocketService {

    ShiprocketSyncResponse syncTracking(Order order);
}
